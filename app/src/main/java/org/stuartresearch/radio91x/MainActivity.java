package org.stuartresearch.radio91x;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.balysv.materialripple.MaterialRippleLayout;
import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.squareup.picasso.Picasso;

import java.util.Vector;

public class MainActivity extends ActionBarActivity {
    private ImageView albumView;
    private TextView songText;
    private TextView artistText;
    private Parser parser;
    private final Vector<SongInfo> songStack = new Vector<>();
    private CardAdapter cardAdapter;
    private boolean toolbarShowing = true;
    private RecyclerView recyclerView;
    private boolean showingFavs = false;
    boolean showingSnackbar = false;
    Streamer streamer;
    private AudioManager.OnAudioFocusChangeListener afChangeListener;
    private AudioManager audioManager;
    private ImageView playPause;
    public static boolean playingElsewhere = false;
    private Toolbar toolBar;
    FavoritesDataSource favoritesDataSource;
    private final MainActivity mainActivity = this;
    private final LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,
            false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playPause = (ImageView) findViewById(R.id.controlImageView);
        new AudioPlayerBroadcastReceiver(playPause);
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
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        int res = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        final MainActivity mainActivity = this;
        streamer = new Streamer(getApplicationContext(), progressBar,
                new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (extra == MediaPlayer.MEDIA_ERROR_IO
                        || what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
                    playPause.callOnClick();
                    hideToolbar();
                    SnackbarManager.show(Snackbar.with(mainActivity)
                            .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE)
                            .actionLabel(getResources().getString(R.string.retry))
                            .text(getResources().getString(R.string.connectionIssues))
                            .color(getResources().getColor(R.color.primary))
                            .actionColor(getResources().getColor(R.color.accent))
                            .actionListener(new ActionClickListener() {
                                @Override
                                public void onActionClicked(Snackbar snackbar) {
                                    playPause.callOnClick();
                                }
                            }).eventListener(new EventListener() {
                        @Override
                        public void onShow(Snackbar snackbar) {
                            progressBar.setIndeterminate(false);
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onShowByReplace(Snackbar snackbar) {

                        }

                        @Override
                        public void onShown(Snackbar snackbar) {

                        }

                        @Override
                        public void onDismiss(Snackbar snackbar) {
                            showToolbar();
                        }

                        @Override
                        public void onDismissByReplace(Snackbar snackbar) {

                        }

                        @Override
                        public void onDismissed(Snackbar snackbar) {

                        }
                    }));
                    return true;
                }
                return false;
            }
        });
        playPause.setTag("play");
        if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) streamer.stop();
        albumView = (ImageView) findViewById(R.id.albumImageView);
        songText = (TextView) findViewById(R.id.songNameTextView);
        artistText = (TextView) findViewById(R.id.ArtistNameTextView);
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag().equals("play")) {
                    streamer.stop();
                    stopParser();
                    songText.setText(getResources().getString(R.string.stationName));
                    artistText.setText(getResources().getString(R.string.LIR));
                    playPause.setImageResource(R.drawable.ic_play_circle_outline_black_36dp);
                    playPause.setTag("pause");
                    hideNotification();
                } else {
                    int res = audioManager.requestAudioFocus(afChangeListener,
                            AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) return;
                    streamer.play();
                    showNotification();
                    startParser();
                    playPause.setImageResource(R.drawable.ic_pause_circle_outline_black_36dp);
                    playPause.setTag("play");
                    showNotification();
                }
            }
        });
        if (res != AudioManager.AUDIOFOCUS_REQUEST_FAILED)
            startParser();
        recyclerView = (RecyclerView) findViewById(R.id.cardList);
        recyclerView.setHasFixedSize(true);
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
            SnackbarManager.show(Snackbar.with(this).text(getResources().getString(R.string.FDU))
                    .actionColor(getResources().getColor(R.color.accent))
                    .color(getResources().getColor(R.color.primary))
                    .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                    .actionLabel(getResources().getString(R.string.close)));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem dialIt = menu.findItem(R.id.call91x);
        MenuItem txtIt = menu.findItem(R.id.text91x);
        final MenuItem showFavs = menu.findItem(R.id.showFavorites);
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
                    showFavs.setIcon(getDrawable(R.drawable.ic_favorite_black_24dp));
                    showingFavs = false;
                    if (songStack.get(songStack.size() - 1).trackId == -666) {
                        cardAdapter = new CardAdapter(songStack, mainActivity, false);
                    } else {
                        cardAdapter = new CardAdapter(songStack, mainActivity, true);
                    }
                    recyclerView.setAdapter(cardAdapter);
                    showToolbar();
                    return true;
                } else {
                    showFavs.setIcon(getDrawable(R.drawable.ic_favorite_red_24dp));
                    showingFavs = true;
                    cardAdapter = new CardAdapter(favoritesDataSource.getFavorites(),
                            mainActivity, false);
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
            songText.setText(getResources().getString(R.string.outOfSync));
            artistText.setText("");
            CardAdapter.playingTopCard = false;
            cardAdapter.notifyItemChanged(0);
            return;
        }
        if (songStack.size() > 0
                && songInfo.trackId == songStack.get(songStack.size() - 1).trackId) {
            parser = new Parser(this);
            parser.songTitle = songInfo.songName;
            parser.artistName = songInfo.artistName;
            parser.execute();
            return;
        }
        if (songStack.size() > 1
                && songInfo.trackId == songStack.get(songStack.size() - 2).trackId) {
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

    private void startParser() {
        parser = new Parser(this);
        parser.running = true;
        parser.execute();

    }

    private void stopParser() {
        parser.cancel(true);
        parser.running = false;
    }

    private void showNotification() {
        Intent pause = new Intent();
        pause.setAction("org.stuartresearch.radio91x.ACTION_PAUSE");
        PendingIntent pausePending = PendingIntent.getBroadcast (this, 0, pause,
                PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder mBuilder = null;
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_radio_white_24dp)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(songText.getText())
                .setContentText(artistText.getText())
                .setColor(getResources().getColor(R.color.primary));

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_HOME);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mBuilder.build();
        RemoteViews smallView = new RemoteViews(this.getPackageName(),
                R.layout.mini_notification_layout);
        smallView.setTextViewText(R.id.miniNotificationSongName, songText.getText());
        smallView.setTextViewText(R.id.miniNotificationArtistName, artistText.getText());
        smallView.setImageViewResource(R.id.miniNotificationButton, R.drawable.ic_pause_black_24dp);
        smallView.setOnClickPendingIntent(R.id.miniNotificationButton, pausePending);
        notification.contentView = smallView;
        notification.priority = Notification.PRIORITY_MAX;
        notification.contentIntent = resultPendingIntent;
        notificationManager.cancel(919191);
        notificationManager.notify(919191, notification);
    }

    private void hideNotification() {
        Intent pause = new Intent();
        pause.setAction("org.stuartresearch.radio91x.ACTION_PLAY");
        PendingIntent pausePending = PendingIntent.getBroadcast (this, 0, pause, 0);
        NotificationCompat.Builder mBuilder = null;
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_radio_white_24dp)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(songText.getText())
                .setContentText(artistText.getText())
                .setColor(getResources().getColor(R.color.primary));

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);
        resultIntent.addCategory(Intent.CATEGORY_HOME);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mBuilder.build();
        RemoteViews smallView = new RemoteViews(this.getPackageName(),
                R.layout.mini_notification_layout);
        smallView.setTextViewText(R.id.miniNotificationSongName,
                getResources().getString(R.string.stationName));
        smallView.setTextViewText(R.id.miniNotificationArtistName,
                getResources().getString(R.string.LIR));
        smallView.setImageViewResource(R.id.miniNotificationButton,
                R.drawable.ic_play_arrow_black_24dp);
        smallView.setOnClickPendingIntent(R.id.miniNotificationButton, pausePending);
        notification.contentView = smallView;
        notification.priority = Notification.PRIORITY_HIGH;
        notification.contentIntent = resultPendingIntent;
        notificationManager.cancel(919191);
        notificationManager.notify(919191, notification);
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

    void showToolbar() {
        toolBar.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
        toolbarShowing = true;
    }

    void hideToolbar() {
        toolBar.animate().translationY(toolBar.getHeight())
                .setInterpolator(new AccelerateInterpolator()).start();
        toolbarShowing = false;
    }

}