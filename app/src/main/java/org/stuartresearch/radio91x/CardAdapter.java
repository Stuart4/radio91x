package org.stuartresearch.radio91x;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

/**
 * Created by jake on 5/13/15.
 */
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.SongInfoHolder> {
    private SongStack songInfoStack;
    private Context context;
    private static FavoritesDataSource dataSource;
    static boolean playingTopCard = false;
    public final MainActivity mainActivity;

    public CardAdapter(SongStack stack, MainActivity mainActivity, boolean playingTopCard) {
        songInfoStack = stack;
        this.context = mainActivity.getApplicationContext();
        CardAdapter.playingTopCard = playingTopCard;
        this.mainActivity = mainActivity;
    }

    public static void setDataSource(FavoritesDataSource newDataSource) {
        dataSource = newDataSource;
    }

    public void setSongInfoStack(SongStack stack) {
        this.songInfoStack = stack;
        if (stack.size() > 0 && stack.get(0).trackId == -666) {
            playingTopCard = false;
        } else {
            playingTopCard = true;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return songInfoStack.size();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void onBindViewHolder(SongInfoHolder songInfoHolder, int pos) {
        if (dataSource == null) {
            songInfoHolder.favoriteSong.setVisibility(View.INVISIBLE);
        } else if (dataSource.isFavorite(songInfoStack.get(pos))) {
            songInfoHolder.favoriteSong.setImageResource(R.drawable.ic_favorite_red_24dp);
        } else {
            songInfoHolder.favoriteSong.setImageResource(R.drawable.ic_favorite_outline_black_24dp);
        }
        SongInfo songInfo = songInfoStack.get(pos);
        if (pos == 0 && playingTopCard) {
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
        if (!songInfo.imageUrl.isEmpty())
            Picasso.with(songInfoHolder.context).load(songInfo.imageUrl)
                    .transform(new AlbumTransformation(songInfoHolder.albumImage))
                    .into(songInfoHolder.albumImage);
        else {
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
        songInfoHolder.favoriteSong.setOnClickListener(new CardClickListenerFavorite(pos));

    }

    @Override
    public SongInfoHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.card_layout, viewGroup, false);
        return new SongInfoHolder(itemView);
    }

    public class SongInfoHolder extends RecyclerView.ViewHolder {
        TextView songText;
        TextView artistText;
        ImageView albumImage;
        View background;
        ImageView equalizer;
        ImageView buySong;
        ImageView previewSong;
        ImageView favoriteSong;

        final Context context;

        public SongInfoHolder(View v) {
            super(v);
            songText = (TextView) v.findViewById(R.id.songNameTextView);
            artistText = (TextView) v.findViewById(R.id.artistNameTextView);
            albumImage = (ImageView) v.findViewById(R.id.albumImageView);
            context = mainActivity;
            background = v.findViewById(R.id.playingBackground);
            equalizer = (ImageView) v.findViewById(R.id.playingEqualizer);
            buySong = (ImageView) v.findViewById(R.id.buySongButton);
            previewSong = (ImageView) v.findViewById(R.id.previewSongButton);
            favoriteSong = (ImageView) v.findViewById(R.id.favoriteSongButton);

        }
    }

    public static class AlbumTransformation implements Transformation {
        final ImageView albumImage;

        public AlbumTransformation(ImageView iv) {
            albumImage = iv;
        }

        @Override
        public Bitmap transform(Bitmap source) {
            int x = 0;
            int y = (source.getHeight() - albumImage.getHeight()) / 2;
            Bitmap result;
            try {
                result = Bitmap.createBitmap(source, x, y >= 0 ? y : 0,
                        Math.min(albumImage.getWidth(), source.getWidth()),
                        Math.min(albumImage.getHeight(), source.getHeight()));
            } catch (Exception e) {
                Log.e("91x", Log.getStackTraceString(e));
                return source;
            }
            if (result != source) {
                source.recycle();
            }
            return result;
        }

        @Override
        public String key() {
            return "square()";
        }
    }


    class CardClickListenerBuy implements View.OnClickListener {
        final int i;

        public CardClickListenerBuy(int i) {
            this.i = i;
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(songInfoStack.get(i).buySong));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    class CardClickListenerPreview implements View.OnClickListener {
        final int i;

        public CardClickListenerPreview(int i) {
            this.i = i;
        }

        @Override
        public void onClick(View v) {
            final MediaPlayer sample = new MediaPlayer();
            final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            MainActivity.playingElsewhere = true;
            final AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
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
            int res = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            SnackbarManager.show(Snackbar.with(v.getContext()).text(String
                    .format("Playing a preview of %s by %s.",
                            songInfoStack.get(i).songName,
                            songInfoStack.get(i).artistName))
                    .actionColor(context.getResources().getColor(R.color.accent))
                    .color(context.getResources().getColor(R.color.primary))
                    .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                    .type(SnackbarType.MULTI_LINE)
                    .actionLabel("STOP")
                    .actionListener(new ActionClickListener() {
                        @Override
                        public void onActionClicked(Snackbar snackbar) {
                            sample.release();
                            audioManager.abandonAudioFocus(afChangeListener);
                            context.sendBroadcast(new Intent("org.stuartresearch.radio91x.SOUND"));
                        }
                    })
                    .eventListener(new EventListener() {
                        @Override
                        public void onShow(Snackbar snackbar) {
                            mainActivity.hideToolbar();
                            mainActivity.showingSnackbar = true;
                        }

                        @Override
                        public void onShowByReplace(Snackbar snackbar) {

                        }

                        @Override
                        public void onShown(Snackbar snackbar) {

                        }

                        @Override
                        public void onDismiss(Snackbar snackbar) {
                            mainActivity.showToolbar();
                            mainActivity.showingSnackbar = false;
                            sample.release();
                        }

                        @Override
                        public void onDismissByReplace(Snackbar snackbar) {

                        }

                        @Override
                        public void onDismissed(Snackbar snackbar) {

                        }
                    }));

            context.sendBroadcast(new Intent("org.stuartresearch.radio91x.NOSOUND"));
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
                        context.sendBroadcast(new Intent("org.stuartresearch.radio91x.SOUND"));
                        MainActivity.playingElsewhere = false;
                        SnackbarManager.dismiss();
                    }
                });
                sample.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        sample.release();
                        context.sendBroadcast(new Intent("org.stuartresearch.radio91x.SOUND"));
                        MainActivity.playingElsewhere = false;
                        SnackbarManager.dismiss();
                        return false;
                    }
                });
            } catch (Exception e) {
                Log.e("91x", Log.getStackTraceString(e));
            }
        }
    }

    class CardClickListenerFavorite implements View.OnClickListener {
        final int i;

        public CardClickListenerFavorite(int i) {
            this.i = i;
        }

        @Override
        public void onClick(View v) {
            final SongInfo songInfo = songInfoStack.get(i);
            final ImageView favoriteSong = (ImageView) v;
            String snackString;
            if (songInfo.favorite) {
                favoriteSong.setImageResource(R.drawable.ic_favorite_outline_black_24dp);
                songInfo.favorite = false;
                snackString = String.format(context.getResources().getString(R.string.favoriteSaved), songInfo.songName, songInfo.artistName);
                dataSource.unfavorite(songInfo);
            } else {
                favoriteSong.setImageResource(R.drawable.ic_favorite_red_24dp);
                songInfo.favorite = true;
                snackString = String.format(context.getResources().getString(R.string.favoriteSaved), songInfo.songName, songInfo.artistName);
                dataSource.favorite(songInfo);
            }
            SnackbarManager.show(Snackbar.with(v.getContext()).text(snackString)
                    .actionColor(context.getResources().getColor(R.color.accent))
                    .color(context.getResources().getColor(R.color.primary))
                    .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                    .actionLabel(context.getResources().getString(R.string.undo))
                    .type(SnackbarType.MULTI_LINE)
                    .actionListener(new ActionClickListener() {
                        @Override
                        public void onActionClicked(Snackbar snackbar) {
                            if (songInfo.favorite) {
                                favoriteSong.setImageResource(R.drawable.ic_favorite_outline_black_24dp);
                                songInfo.favorite = false;
                                dataSource.unfavorite(songInfo);
                            } else {
                                favoriteSong.setImageResource(R.drawable.ic_favorite_red_24dp);
                                songInfo.favorite = true;
                                dataSource.favorite(songInfo);
                            }
                        }
                    })
                    .eventListener(new EventListener() {
                        @Override
                        public void onShow(Snackbar snackbar) {
                            mainActivity.hideToolbar();
                            mainActivity.showingSnackbar = true;
                        }

                        @Override
                        public void onShowByReplace(Snackbar snackbar) {

                        }

                        @Override
                        public void onShown(Snackbar snackbar) {

                        }

                        @Override
                        public void onDismiss(Snackbar snackbar) {
                            mainActivity.showToolbar();
                            mainActivity.showingSnackbar = false;
                        }

                        @Override
                        public void onDismissByReplace(Snackbar snackbar) {

                        }

                        @Override
                        public void onDismissed(Snackbar snackbar) {

                        }
                    }));
        }
    }

}


