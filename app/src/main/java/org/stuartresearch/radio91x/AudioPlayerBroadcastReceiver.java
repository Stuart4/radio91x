package org.stuartresearch.radio91x;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Created by jake on 5/15/15.
 */
public class AudioPlayerBroadcastReceiver extends BroadcastReceiver implements ServiceConnection{
    private static MainActivity mainActivity;
    private static RadioService radioService;

    public static void setActivity (MainActivity act) {
        mainActivity = act;
    }

    public static void setService (RadioService srv) {
        radioService = srv;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (mainActivity != null) {
            if (action.equals("org.stuartresearch.radio91x.LOADED")) {
                mainActivity.streamLoaded();
            } else if (action.equals("org.stuartresearch.radio91x.LOADING")) {
                mainActivity.streamLoading();
            } else if (action.equals("org.stuartresearch.radio91x.ERROR")) {
                mainActivity.streamError();
            } else if (action.equals("org.stuartresearch.radio91x.PLAYING")) {
                mainActivity.streamPlaying();
            } else if (action.equals("org.stuartresearch.radio91x.STOPPED")) {
                mainActivity.streamStopped();
            }
        }

        if (radioService != null) {
            if (action.equals("org.stuartresearch.radio91x.PLAY")) {
                radioService.play();
            } else if (action.equals("org.stuartresearch.radio91x.PAUSE")) {
                radioService.stop();
            } else if (action.equals("org.stuartresearch.radio91x.NOSOUND")) {
                radioService.noSound();
            } else if (action.equals("org.stuartresearch.radio91x.NOSOUND")) {
                radioService.sound();
            } else if (action.equals("android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY")) {
                radioService.stop();
            }
        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}

