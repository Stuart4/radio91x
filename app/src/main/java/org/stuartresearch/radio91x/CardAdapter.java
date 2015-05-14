package org.stuartresearch.radio91x;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

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
        SongInfo songInfo = songInfoStack.get(getItemCount() - 1 - i);
        songInfoHolder.songText.setText(songInfo.songName);
        songInfoHolder.artistText.setText(songInfo.artistName);
        if (songInfo.imageUrl.length() > 0)
            Picasso.with(songInfoHolder.context).load(songInfo.imageUrl).into(songInfoHolder.albumImage);

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
        Context context;
        public SongInfoHolder(View v) {
            super(v);
            songText = (TextView) v.findViewById(R.id.songNameTextView);
            artistText = (TextView) v.findViewById(R.id.artistNameTextView);
            albumImage = (ImageView) v.findViewById(R.id.albumImageView);
            context = v.getContext();
        }
    }
}


