package org.stuartresearch.radio91x;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.util.Stack;

/**
 * Created by jake on 5/13/15.
 */
public class CardAdapter extends RecyclerView.Adapter<CardAdapter.SongInfoHolder> {
    private Stack<SongInfo> songInfoStack;

    public CardAdapter(Stack<SongInfo> stack) {
        songInfoStack = stack;
    }

    @Override
    public int getItemCount() {
       return songInfoStack.size();
    }

    @Override
    public void onBindViewHolder(SongInfoHolder songInfoHolder, int i) {
        int pos = getItemCount() - 1 - i;
        SongInfo songInfo = songInfoStack.get(pos);
        if (i == 0) {
            songInfoHolder.background.setVisibility(View.VISIBLE);
            songInfoHolder.equalizer.setVisibility(View.VISIBLE);
            songInfoHolder.albumImage.setAlpha(0.25f);
        } else {
            songInfoHolder.background.setVisibility(View.GONE);
            songInfoHolder.equalizer.setVisibility(View.GONE);
            songInfoHolder.albumImage.setAlpha(1f);
        }
        songInfoHolder.songText.setText(songInfo.songName);
        songInfoHolder.artistText.setText(songInfo.artistName);
        if (songInfo.imageUrl.length() > 0)
            Picasso.with(songInfoHolder.context).load(songInfo.imageUrl).transform(new AlbumTransformation(songInfoHolder.albumImage)).into(songInfoHolder.albumImage);

    }

    @Override
    public SongInfoHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.card_layout, viewGroup, false);
        return new SongInfoHolder(itemView);
    }

    public static class SongInfoHolder extends RecyclerView.ViewHolder {
        protected TextView songText;
        protected TextView artistText;
        protected ImageView albumImage;
        protected View background;
        protected ImageView equalizer;
        Context context;
        public SongInfoHolder(View v) {
            super(v);
            songText = (TextView) v.findViewById(R.id.songNameTextView);
            artistText = (TextView) v.findViewById(R.id.artistNameTextView);
            albumImage = (ImageView) v.findViewById(R.id.albumImageView);
            context = v.getContext();
            background = (View) v.findViewById(R.id.playingBackground);
            equalizer = (ImageView) v.findViewById(R.id.playingEqualizer);
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
            Bitmap result = Bitmap.createBitmap(source, x, y >= 0 ? y : 0, Math.min(albumImage.getWidth(), source.getWidth()), albumImage.getHeight());
            if (result != source) {
                source.recycle();
            }
            return result;
        }

        @Override public String key() { return "square()"; }
    }
}


