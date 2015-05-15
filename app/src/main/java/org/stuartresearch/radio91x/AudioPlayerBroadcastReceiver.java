package org.stuartresearch.radio91x;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;

/**
 * Created by jake on 5/15/15.
 */
public class AudioPlayerBroadcastReceiver extends BroadcastReceiver {
    static ImageView playPause;

    public AudioPlayerBroadcastReceiver(ImageView image) {
        playPause = image;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if(action.equals("org.stuartresearch.radio91x.ACTION_PLAY")) {
            playPause.callOnClick();
        } else if (action.equals("org.stuartresearch.radio91x.ACTION_PAUSE")) {
            playPause.callOnClick();
        }

    }
}

