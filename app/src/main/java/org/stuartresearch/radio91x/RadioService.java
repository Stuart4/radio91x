package org.stuartresearch.radio91x;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;

/**
 * Created by jake on 5/18/15.
 */
public class RadioService extends Service implements MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener {
    private final Uri uri;
    private final IBinder mBinder = new LocalBinder();
    private MediaPlayer mediaPlayer = null;
    private Parser parser;
    private boolean parserRunning = false;
    private SongInfo currentSong = new SongInfo();
    private boolean playing = false;
    private boolean prepared = false;
    private boolean preparing = false;
    private AudioManager audioManager = null;
    private NotificationManager notificationManager;
    WifiManager.WifiLock wifiLock;
    private static boolean running = false;
    private int binds = 0;
    protected SongStack songStack = new SongStack(50);
    private SharedPreferences sharedPreferences;

    public class LocalBinder extends Binder {
        RadioService getService() {
            // Return this instance of LocalService so clients can call public methods
            return RadioService.this;
        }
    }

    @Override
    public void onCreate() {
        running = true;
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "radio91x_lock");
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (audioManager == null) {
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        }
        AudioPlayerBroadcastReceiver.setService(this);
        currentSong.songName = "91x";
        currentSong.artistName = "Local. Independent. Radio";
        currentSong.trackId = -666;
        showNotification();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        binds++;
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        play();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mediaPlayer.release();
        mediaPlayer = null;
        running = false;
        AudioPlayerBroadcastReceiver.setService(null);
        parser.removeRadioService();
        super.onDestroy();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            stop(false);
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            noSound();
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            sound();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (extra == MediaPlayer.MEDIA_ERROR_IO
                || what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
            mediaError();
            return true;
        }
        return false;
    }

    public RadioService() {

        Uri.Builder uBuilder = new Uri.Builder();
        uBuilder.scheme("http")
                .authority("playerservices.streamtheworld.com")
                .appendPath("api")
                .appendPath("livestream-redirect")
                .appendPath("XTRAFM.mp3");
        uri = uBuilder.build();


    }


    public void play() {
        int res = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            onStopped();
            return;
        }

        showNotification();
        if (prepared) {
            mediaPlayer.start();
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            wifiLock.acquire();
            playing = true;
            preparing = false;
            showNotification();
            stopParser();
            startParser();
        } else {
            onLoading();
            if (mediaPlayer != null)
                mediaPlayer.release();
            mediaPlayer = null;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            wifiLock.acquire();
            try {
                mediaPlayer.setDataSource(this, uri);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        prepared = true;
                        mp.start();
                        playing = true;
                        preparing = false;
                        onLoaded();
                        onPlaying();
                        stopParser();
                        startParser();
                    }
                });
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.prepareAsync();
                preparing = true;
                onLoading();
            } catch (IOException e) {
                onStopped();
                mediaError();
            }
        }
        startParser();
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

    public void stop(boolean fromReceiver) {
        mediaPlayer.reset();
        prepared = false;
        playing = false;
        preparing = false;

        if(!fromReceiver) {
            onLoaded();
            onStopped();
        }

        stopParser();
        hideNotification();
        wifiLock.setReferenceCounted(false);
        wifiLock.release();
        if (binds == 0) stopSelf();
        Log.e("91x", "will be shutting down!\n");
    }

    public boolean isPlaying() {
        return playing;
    }

    public void onLoading() {
        sendBroadcast(new Intent("org.stuartresearch.radio91x.LOADING"));
    }

    public void onLoaded() {
        sendBroadcast(new Intent("org.stuartresearch.radio91x.LOADED"));
    }

    public void mediaError() {
        sendBroadcast(new Intent("org.stuartresearch.radio91x.ERROR"));
    }

    public void onPlaying() {
        sendBroadcast(new Intent("org.stuartresearch.radio91x.PLAYING"));
    }

    public void onStopped() {
        sendBroadcast(new Intent("org.stuartresearch.radio91x.STOPPED"));
    }

    public static boolean isRunning() {
        return running;
    }

    private void showNotification() {
        Intent pause = new Intent();
        pause.setAction("org.stuartresearch.radio91x.PAUSE");
        PendingIntent pausePending = PendingIntent.getBroadcast(this, 0, pause, 0);
        NotificationCompat.Builder mBuilder = null;
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_radio_white_24dp)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(getResources().getColor(R.color.primary));

        Intent reopenIntent = new Intent(this, MainActivity.class);
        reopenIntent.setAction(Intent.ACTION_MAIN);
        reopenIntent.addCategory(Intent.CATEGORY_HOME);
        PendingIntent reopenPending = PendingIntent.getActivity(this, 0, reopenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(reopenPending);
        Notification notification = mBuilder.build();
        RemoteViews smallView = new RemoteViews(this.getPackageName(),
                R.layout.mini_notification_layout);
        smallView.setTextViewText(R.id.miniNotificationSongName, currentSong.songName);
        smallView.setTextViewText(R.id.miniNotificationArtistName, currentSong.artistName);
        smallView.setImageViewResource(R.id.miniNotificationButton,
                R.drawable.ic_pause_black_24dp);
        smallView.setOnClickPendingIntent(R.id.miniNotificationButton, pausePending);
        notification.contentView = smallView;
        notification.priority = Notification.PRIORITY_HIGH;
        notification.contentIntent = reopenPending;
        notificationManager.cancel(919191);
        startForeground(919191, notification);
    }

    private void hideNotification() {
        stopForeground(true);
        Intent play = new Intent();
        play.setAction("org.stuartresearch.radio91x.PLAY");
        PendingIntent playPending = PendingIntent.getBroadcast (this, 0, play, 0);
        NotificationCompat.Builder mBuilder = null;
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_radio_white_24dp)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle("91x")
                .setContentText("Local. Independent. Radio.")
                .setColor(getResources().getColor(R.color.primary));

        Intent reopenIntent = new Intent(this, MainActivity.class);
        reopenIntent.setAction(Intent.ACTION_MAIN);
        reopenIntent.addCategory(Intent.CATEGORY_HOME);
        PendingIntent reopenPending = PendingIntent.getActivity(this, 0, reopenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(reopenPending);
        Notification notification = mBuilder.build();
        RemoteViews smallView = new RemoteViews(this.getPackageName(),
                R.layout.mini_notification_layout);
        smallView.setTextViewText(R.id.miniNotificationSongName,
                getResources().getString(R.string.stationName));
        smallView.setTextViewText(R.id.miniNotificationArtistName,
                getResources().getString(R.string.LIR));
        smallView.setImageViewResource(R.id.miniNotificationButton,
                R.drawable.ic_play_arrow_black_24dp);
        smallView.setOnClickPendingIntent(R.id.miniNotificationButton, playPending);
        notification.contentView = smallView;
        notification.priority = Notification.PRIORITY_HIGH;
        notification.contentIntent = reopenPending;
        notificationManager.cancel(919191);
        notificationManager.notify(919191, notification);
    }

    public void updateSongInfo(SongInfo songInfo) {

        if (songStack.size() > 0
                && songInfo.trackId == songStack.get(0).trackId && parserRunning) {
            new Parser(this, currentSong.songName, currentSong.artistName).execute();
            return;
        }
        if (songStack.size() > 1
                && songInfo.trackId == songStack.get(1).trackId && parserRunning) {
            new Parser(this, currentSong.songName, currentSong.artistName).execute();
            return;
        }

        if (songInfo.trackId != -666) {
            sound();
            CardAdapter.playingTopCard = true;
            songStack.insert(songInfo);
            currentSong = songInfo;
            showNotification();
        } else {
            if (sharedPreferences.getBoolean("muteAds", false)) {
                noSound();
            }

        }
        if (parserRunning)
            new Parser(this, currentSong.songName, currentSong.artistName).execute();
    }

    private void startParser() {
        parser = new Parser(this, currentSong.songName, currentSong.artistName);
        parser.songTitle = currentSong.songName;
        parser.artistName = currentSong.artistName;
        parser.running = true;
        parser.execute();
        parserRunning = true;

    }

    private void stopParser() {
        parser.cancel(true);
        parserRunning = false;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (binds == 1 && !playing) {
            Log.e("91x", "KILLED THE SERVICE");
            stopSelf();
        }
        binds--;
        return super.onUnbind(intent);
    }

    public boolean isPreparing() {
        return preparing;
    }

    public SongInfo getCurrentSong() {
        return currentSong;
    }

}
