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
class Streamer {
    final private Uri uri;
    private MediaPlayer mediaPlayer;
    private boolean playing = false;
    private boolean prepared = false;
    final private ProgressBar progressBar;
    final private Context context;
    final private MediaPlayer.OnErrorListener onErrorListener;

    public Streamer(Context context, final ProgressBar progressBar,
                    MediaPlayer.OnErrorListener onErrorListener) {
        this.progressBar = progressBar;
        this.context = context;
        this.onErrorListener = onErrorListener;
        Uri.Builder uBuilder = new Uri.Builder();
        uBuilder.scheme("http")
                .authority("playerservices.streamtheworld.com")
                .appendPath("api")
                .appendPath("livestream-redirect")
                .appendPath("XTRAFM.mp3");
        uri = uBuilder.build();
        play();
    }

    public void play() {
        if (prepared) {
            mediaPlayer.start();
            playing = true;
        } else {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(true);
            if (mediaPlayer != null)
                mediaPlayer.release();
            mediaPlayer = null;
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(context, uri);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        prepared = true;
                        mp.start();
                        playing = true;
                        progressBar.setIndeterminate(false);
                        progressBar.setVisibility(View.GONE);
                    }
                });
                mediaPlayer.setOnErrorListener(onErrorListener);
                mediaPlayer.prepareAsync();
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void noSound() {
        mediaPlayer.setVolume(0, 0);
    }

    public void sound() {
        mediaPlayer.setVolume(1, 1);
    }

    public void kill() {
        mediaPlayer.release();
        prepared = false;
        playing = false;
    }

    public void stop() {
        mediaPlayer.reset();
        prepared = false;
        playing = false;
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(false);
    }

    public boolean isPlaying() {
        return playing;
    }
}
