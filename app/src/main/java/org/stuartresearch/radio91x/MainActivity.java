package org.stuartresearch.radio91x;

import android.app.MediaRouteButton;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataEditor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Stack;
import java.util.Vector;

public class MainActivity extends ActionBarActivity {
    ImageView albumView;
    TextView songText;
    TextView artistText;
    Parser parser;
    Vector<SongInfo> songStack = new Vector<>();
    CardAdapter cardAdapter;
    boolean toolbarShowing = true;
    RecyclerView recyclerView;
    boolean showingFavs = false;
    boolean showingSnackbar = false;
    Streamer streamer;
    AudioManager.OnAudioFocusChangeListener afChangeListener;
    AudioManager audioManager;
    ImageView playPause;
    public static boolean playingElsewhere = false;
    Toolbar toolBar;
    FavoritesDataSource favoritesDataSource;
    final MainActivity mainActivity = this;
    final LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playPause = (ImageView) findViewById(R.id.controlImageView);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (playingElsewhere) return;
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    if (streamer.isPlaying()) playPause.callOnClick();
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    streamer.noSound();
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    streamer.sound();
                }
            }
        };
        int res = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        streamer = new Streamer(getApplicationContext(),
                (ProgressBar) findViewById(R.id.progressBar), playPause);
        if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) streamer.stop();
        albumView = (ImageView) findViewById(R.id.albumImageView);
        songText = (TextView) findViewById(R.id.songNameTextView);
        artistText = (TextView) findViewById(R.id.ArtistNameTextView);
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (streamer.isPlaying()) {
                    streamer.stop();
                    stopParser();
                    playPause.setImageResource(R.drawable.ic_play_arrow_black_18dp);
                    hideNotification();
                } else {
                    int res = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) return;
                    streamer.play();
                    showNotification();
                    startParser();
                    playPause.setImageResource(R.drawable.ic_pause_black_18dp);
                    showNotification();
                }
            }
        });
        new AudioPlayerBroadcastReceiver(playPause);
        if (res != AudioManager.AUDIOFOCUS_REQUEST_FAILED)
            startParser();
        recyclerView = (RecyclerView) findViewById(R.id.cardList);
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(lm);
        cardAdapter = new CardAdapter(songStack, this, false);
        recyclerView.setAdapter(cardAdapter);
        if (Build.VERSION.SDK_INT >20)
            getWindow().setNavigationBarColor(getResources().getColor(R.color.primary_dark));
        toolBar =
                (android.support.v7.widget.Toolbar) findViewById(R.id.lowerToolbar);
        ObservableRecyclerView orv = (ObservableRecyclerView) recyclerView;
        orv.setScrollViewCallbacks(new ObservableScrollViewCallbacks() {
            @Override
            public void onScrollChanged(int i, boolean b, boolean b1) {
            }

            @Override
            public void onDownMotionEvent() {
            }

            @Override
            public void onUpOrCancelMotionEvent(ScrollState scrollState) {
                if (scrollState == ScrollState.UP && toolbarShowing && !showingSnackbar) {
                    hideToolbar();

                } else if (scrollState == ScrollState.DOWN && !toolbarShowing && !showingSnackbar) {
                    showToolbar();
                }
            }
        });
        favoritesDataSource = new FavoritesDataSource(getApplicationContext());
        try {
            favoritesDataSource.open();
            CardAdapter.setDataSource(favoritesDataSource);
        } catch (Exception e) {
            SnackbarManager.show(Snackbar.with(this).text("Favorites Database Unavailable")
                    .actionColor(getResources().getColor(R.color.accent))
                    .color(getResources().getColor(R.color.primary))
                    .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                    .actionLabel("CLOSE"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem dialIt = (MenuItem) menu.findItem(R.id.call91x);
        MenuItem txtIt = (MenuItem) menu.findItem(R.id.text91x);
        final MenuItem showFavs = (MenuItem) menu.findItem(R.id.showFavorites);
        dialIt.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:18585701919"));
                    startActivity(intent);
                } catch (Exception e) {
                    return false;
                }
                return true;
            }
        });
        txtIt.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.fromParts("sms", String.valueOf(33911), null)));
                } catch (Exception e) {
                    return false;
                }
                return true;
            }
        });
        showFavs.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //TODO
                if (showingFavs) {
                    showFavs.setIcon(getResources().getDrawable(R.drawable.ic_favorite_black_18dp));
                    showingFavs = false;
                    cardAdapter = new CardAdapter(songStack, mainActivity, false);
                    recyclerView.setAdapter(cardAdapter);
                    showToolbar();
                    return true;
                } else {
                    showFavs.setIcon(getResources().getDrawable(R.drawable.ic_favorite_red_18dp));
                    showingFavs = true;
                    cardAdapter = new CardAdapter(favoritesDataSource.getFavorites(), mainActivity, true);
                    recyclerView.setAdapter(cardAdapter);
                    showToolbar();
                    return true;
                }
            }

        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateSongInfo(SongInfo songInfo) {
        if (songInfo.trackId == -655) {
            songText.setText("Out of Sync");
            artistText.setText("");
            CardAdapter.playingTopCard = false;
            cardAdapter.notifyItemChanged(0);
            return;
        }
        if (songStack.size() > 0 && songInfo.trackId == songStack.get(songStack.size() - 1).trackId) {
            parser = new Parser(this);
            parser.songTitle = songInfo.songName;
            parser.artistName = songInfo.artistName;
            parser.execute();
            return;
        }
        if (songStack.size() > 1 && songInfo.trackId == songStack.get(songStack.size() - 2).trackId) {
            parser = new Parser(this);
            parser.songTitle = songInfo.songName;
            parser.artistName = songInfo.artistName;
            parser.execute();
            return;
        }
        songText.setText(songInfo.songName);
        artistText.setText(songInfo.artistName);
        if (songInfo.imageUrl.length() > 0) {
            Picasso.with(this).load(songInfo.imageUrl)
                    .into(albumView);
            albumView.setVisibility(View.VISIBLE);
        } else {
            albumView.setVisibility(View.GONE);
        }
        parser = new Parser(this);
        parser.songTitle = songInfo.songName;
        parser.artistName = songInfo.artistName;
        if (songInfo.trackId != -666) {
            CardAdapter.playingTopCard = true;
            songStack.add(songInfo);
            cardAdapter.notifyItemInserted(0);
            cardAdapter.notifyItemChanged(1);
            if (lm.findFirstCompletelyVisibleItemPosition() == 0)
                recyclerView.smoothScrollToPosition(0);
        } else {
            CardAdapter.playingTopCard = false;
            cardAdapter.notifyItemChanged(0);
        }
        parser.execute();
        showNotification();
    }

    public void startParser() {
        parser = new Parser(this);
        parser.running = true;
        parser.execute();

    }

    public void stopParser() {
        parser.cancel(true);
        parser.running = false;
    }

    public void showNotification() {
        Intent pause = new Intent();
        pause.setAction("org.stuartresearch.radio91x.ACTION_PAUSE");
        PendingIntent pausePending = PendingIntent.getBroadcast (this, 0, pause, 0);
        NotificationCompat.Builder mBuilder = null;
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_music_note_black_18dp)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(songText.getText())
                .setContentText(artistText.getText())
                .setColor(getResources().getColor(R.color.primary));

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mBuilder.build();
        RemoteViews smallView = new RemoteViews(this.getPackageName(),
                R.layout.mini_notification_layout);
        smallView.setTextViewText(R.id.miniNotificationSongName, songText.getText());
        smallView.setTextViewText(R.id.miniNotificationArtistName, artistText.getText());
        smallView.setImageViewResource(R.id.miniNotificationButton, R.drawable.ic_pause_black_18dp);
        smallView.setOnClickPendingIntent(R.id.miniNotificationButton, pausePending);
        notification.contentView = smallView;
        notificationManager.notify(919191, notification);

    }

    public void hideNotification() {
        Intent pause = new Intent();
        pause.setAction("org.stuartresearch.radio91x.ACTION_PLAY");
        PendingIntent pausePending = PendingIntent.getBroadcast (this, 0, pause, 0);
        NotificationCompat.Builder mBuilder = null;
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_music_note_black_18dp)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(songText.getText())
                .setContentText(artistText.getText())
                .setColor(getResources().getColor(R.color.primary));

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mBuilder.build();
        RemoteViews smallView = new RemoteViews(this.getPackageName(),
                R.layout.mini_notification_layout);
        smallView.setTextViewText(R.id.miniNotificationSongName, "91x");
        smallView.setTextViewText(R.id.miniNotificationArtistName, "Local. Independent. Radio");
        smallView.setImageViewResource(R.id.miniNotificationButton, R.drawable.ic_play_arrow_black_18dp);
        smallView.setOnClickPendingIntent(R.id.miniNotificationButton, pausePending);
        notification.contentView = smallView;
        notificationManager.notify(919191, notification);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        streamer.kill();
        stopParser();
        hideNotification();
        favoritesDataSource.close();
        Log.d("91x", "DESTROYED!!!");
    }

    @Override
    protected void onNewIntent(Intent intent) {

    }

    protected void showToolbar() {
        toolBar.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
        toolbarShowing = true;
    }

    protected void hideToolbar() {
        toolBar.animate().translationY(toolBar.getHeight()).setInterpolator(new AccelerateInterpolator()).start();
        toolbarShowing = false;
    }

}