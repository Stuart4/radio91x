package org.stuartresearch.radio91x;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.IOException;
import java.util.Stack;
import java.util.Vector;

/**
 * Created by jake on 5/13/15.
 */
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.SongInfoHolder> {
    private Vector<SongInfo> songInfoStack;
    Context context;

    public CardAdapter(Vector<SongInfo> stack, Context context) {
        songInfoStack = stack;
        this.context = context;
    }

    @Override
    public int getItemCount() {
       return songInfoStack.size();
    }

    @Override
    public void onBindViewHolder(SongInfoHolder songInfoHolder, int i) {
        int pos = songInfoStack.size() - 1 - i;
        SongInfo songInfo = songInfoStack.get(songInfoStack.size() - 1 - i);
        if (i == 0) {
            songInfoHolder.background.setVisibility(View.VISIBLE);
            songInfoHolder.equalizer.setVisibility(View.VISIBLE);
            songInfoHolder.albumImage.setAlpha(.25f);
        } else {
            songInfoHolder.background.setVisibility(View.GONE);
            songInfoHolder.equalizer.setVisibility(View.GONE);
            songInfoHolder.albumImage.setAlpha(1f);
        }
        songInfoHolder.songText.setText(songInfo.songName);
        songInfoHolder.artistText.setText(songInfo.artistName);
        if (songInfo.imageUrl.length() > 0)
            Picasso.with(songInfoHolder.context).load(songInfo.imageUrl)
                    .placeholder(context.getResources().getDrawable(R.drawable.background))
                    .transform(new AlbumTransformation(songInfoHolder.albumImage))
                    .into(songInfoHolder.albumImage);
        else {
            Picasso.with(songInfoHolder.context).
                    load(R.drawable.background).
                    transform(new AlbumTransformation(songInfoHolder.albumImage)).
                    into(songInfoHolder.albumImage);
        }
        if (songInfoStack.get(pos).buySong.isEmpty()) {
            songInfoHolder.buySong.setVisibility(View.INVISIBLE);
        } else {
            songInfoHolder.buySong.setOnClickListener(new CardClickListenerBuy(pos));
        }
        if (songInfoStack.get(pos).songSample.isEmpty()) {
            songInfoHolder.previewSong.setVisibility(View.INVISIBLE);
        } else {
            songInfoHolder.previewSong.setOnClickListener(new CardClickListenerPreview(pos));
        }
        songInfoHolder.favoriteSong.setOnClickListener(new CardClickListenerBuy(pos));

    }

    @Override
    public SongInfoHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.card_layout, viewGroup, false);
        return new SongInfoHolder(itemView);
    }

    public static class SongInfoHolder extends RecyclerView.ViewHolder {
        protected TextView songText;
        protected TextView artistText;
        protected ImageView albumImage;
        protected View background;
        protected ImageView equalizer;
        protected ImageView buySong;
        protected ImageView previewSong;
        protected ImageView favoriteSong;

        Context context;
        public SongInfoHolder(View v) {
            super(v);
            songText = (TextView) v.findViewById(R.id.songNameTextView);
            artistText = (TextView) v.findViewById(R.id.artistNameTextView);
            albumImage = (ImageView) v.findViewById(R.id.albumImageView);
            context = v.getContext();
            background = (View) v.findViewById(R.id.playingBackground);
            equalizer = (ImageView) v.findViewById(R.id.playingEqualizer);
            buySong = (ImageView) v.findViewById(R.id.buySongButton);
            previewSong = (ImageView) v.findViewById(R.id.previewSongButton);
            favoriteSong = (ImageView) v.findViewById(R.id.favoriteSongButton);

        }
    }
    public static class AlbumTransformation implements Transformation {
        ImageView albumImage;

        public AlbumTransformation(ImageView iv) {
            albumImage = iv;
        }

        @Override public Bitmap transform(Bitmap source) {
            int x = 0;
            int y = (source.getHeight() / 2) - albumImage.getHeight();
            Bitmap result = Bitmap.createBitmap(source, x, y >= 0 ? y : 0,
                    Math.min(albumImage.getWidth(), source.getWidth()), albumImage.getHeight());
            if (result != source) {
                source.recycle();
            }
            return result;
        }

        @Override public String key() { return "square()"; }
    }


    class CardClickListenerBuy implements View.OnClickListener {
        int i;

        public CardClickListenerBuy(int i) {
            this.i = i;
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(songInfoStack.get(i).buySong));
            context.startActivity(intent);
        }
    }
    class CardClickListenerPreview implements View.OnClickListener {
        int i;

        public CardClickListenerPreview(int i) {
            this.i = i;
        }

        @Override
        public void onClick(View v) {
            final MediaPlayer sample = new MediaPlayer();
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            ((MainActivity) context).playingElsewhere = true;
            AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    try {
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            sample.setVolume(0, 0);
                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK ||
                                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                            sample.setVolume(0, 0);
                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                            sample.setVolume(1, 1);
                        }
                    } catch (IllegalStateException e) {
                        //sample has already been released, move on
                    }
                }
            };
            int res = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

            SnackbarManager.show(Snackbar.with(context).text(String
                    .format("Playing a preview of %s by %s.",
                            songInfoStack.get(i).songName,
                            songInfoStack.get(i).artistName))
                    .actionColor(context.getResources().getColor(R.color.accent))
                    .color(context.getResources().getColor(R.color.primary))
                    .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                    .actionLabel("STOP")
                    .actionListener(new ActionClickListener() {
                        @Override
                        public void onActionClicked(Snackbar snackbar) {
                            sample.release();
                            ((MainActivity) context).streamer.sound();
                        }
                    }));
            ((MainActivity) context). streamer.noSound();
            try {
                sample.setDataSource(context, Uri.parse(songInfoStack.get(i).songSample));
                sample.prepareAsync();
                sample.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        sample.start();
                    }
                });
                sample.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        sample.release();
                        ((MainActivity) context).streamer.sound();
                        ((MainActivity) context).playingElsewhere = false;
                        SnackbarManager.dismiss();
                    }
                });
                sample.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        sample.release();
                        ((MainActivity) context).streamer.sound();
                        ((MainActivity) context).playingElsewhere = false;
                        SnackbarManager.dismiss();
                        return false;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    class CardClickListenerFavorite implements View.OnClickListener {
        int i;

        public CardClickListenerFavorite(int i) {
            this.i = i;
        }

        @Override
        public void onClick(View v) {
        }
    }

}


