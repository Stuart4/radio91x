package org.stuartresearch.radio91x;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.widget.ProgressBar;

/**
 * Created by jake on 5/18/15.
 */
public class RadioService extends Service {
    MediaPlayer.OnErrorListener onErrorListener;
    ProgressBar progressBar;
    public RadioService(MediaPlayer.OnErrorListener onErrorListener, ProgressBar progressBar) {
        super();
        this.onErrorListener = onErrorListener;
        this.progressBar = progressBar;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Streamer s = new Streamer(getApplicationContext(), progressBar, onErrorListener);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }
}
