package org.stuartresearch.radio91x;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.IOException;

/**
 * Created by jake on 5/12/15.
 */
public class Streamer {
    private Uri uri;
    private MediaPlayer mediaPlayer;
    private boolean playing = false;
    private boolean prepared = false;
    private ProgressBar progressBar;
    private ImageView playPause;

    public Streamer(Context context, final ProgressBar progressBar, final ImageView playPause) {
        this.progressBar = progressBar;
        this.playPause = playPause;
        playPause.setImageResource(android.R.drawable.ic_media_pause);
        Uri.Builder uBuilder = new Uri.Builder();
        uBuilder.scheme("http")
                .authority("playerservices.streamtheworld.com")
                .appendPath("api")
                .appendPath("livestream-redirect")
                .appendPath("XTRAFM.mp3");
        uri = uBuilder.build();
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, uri);
            mediaPlayer.prepareAsync();
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                prepared = true;
                mp.start();
                playing = true;
                progressBar.setIndeterminate(false);
                progressBar.setVisibility(View.GONE);
                playPause.setImageResource(android.R.drawable.ic_media_pause);
            }
        });
    }

    public void play() {
        if (prepared) {
            mediaPlayer.start();
            playing = true;
            System.out.println(mediaPlayer.getTrackInfo());
            playPause.setImageResource(android.R.drawable.ic_media_pause);
        }
        //System.out.println(mediaPlayer.getTrackInfo()[0]);

    }

    public void pause() {
        if (!prepared || !playing) return;
        mediaPlayer.pause();
        playing = false;
        playPause.setImageResource(android.R.drawable.ic_media_play);

    }

    public void reset() {
        mediaPlayer.reset();
        mediaPlayer.prepareAsync();
        prepared = false;
        playing = false;
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
        playPause.setImageResource(android.R.drawable.ic_media_play);
    }

    public void stop() {
        if (!prepared) return;
        mediaPlayer.stop();
        prepared = false;
        playing = false;
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(false);
        playPause.setImageResource(android.R.drawable.ic_media_play);
    }

    public boolean isPlaying() {
        return playing;
    }
}
