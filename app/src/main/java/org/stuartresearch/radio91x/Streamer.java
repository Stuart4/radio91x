package org.stuartresearch.radio91x;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
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
    private boolean preparing = false;
    private ProgressBar progressBar;
    private ImageView playPause;
    Context context;

    public Streamer(Context context, final ProgressBar progressBar, final ImageView playPause) {
        this.progressBar = progressBar;
        this.playPause = playPause;
        this.context = context;
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
        } else if (!preparing) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(true);
            mediaPlayer = null;
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(context, uri);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        prepared = true;
                        mp.start();
                        preparing = false;
                        playing = true;
                        progressBar.setIndeterminate(false);
                        progressBar.setVisibility(View.GONE);
                    }
                });
                mediaPlayer.prepareAsync();
                preparing = true;
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
        if (!prepared) return;
        mediaPlayer.stop();
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
