package org.stuartresearch.radio91x;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;
import com.squareup.picasso.Picasso;

import java.util.Vector;

public class MainActivity extends ActionBarActivity implements ServiceConnection,
        View.OnClickListener {

    private ImageView albumView;
    private TextView songText;
    private TextView artistText;
    ProgressBar progressBar;
    private Parser parser;
    private final Vector<SongInfo> songStack = new Vector<>();
    private CardAdapter cardAdapter;
    private boolean toolbarShowing = true;
    private RecyclerView recyclerView;
    private boolean showingFavs = false;
    boolean showingSnackbar = false;
    private AudioManager audioManager;
    private ImageView playPause;
    public static boolean playingElsewhere = false;
    private Toolbar toolBar;
    FavoritesDataSource favoritesDataSource;
    private final MainActivity mainActivity = this;
    private final LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,
            false);
    private SharedPreferences sharedPreferences;
    protected RadioService.LocalBinder localBinder;
    private boolean bound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playPause = (ImageView) findViewById(R.id.controlImageView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        playPause.setTag("play");
        albumView = (ImageView) findViewById(R.id.albumImageView);
        songText = (TextView) findViewById(R.id.songNameTextView);
        artistText = (TextView) findViewById(R.id.ArtistNameTextView);
        final ServiceConnection conn = this;
        playPause.setOnClickListener(this);
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


        Intent intent = new Intent(getApplicationContext(), RadioService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
        startService(intent);
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
                if (showingFavs) {
                    showFavs.setIcon(getDrawable(R.drawable.ic_favorite_black_24dp));
                    showingFavs = false;
                    if (songStack.get(songStack.size() - 1).trackId == -666) {
                        cardAdapter = new CardAdapter(songStack, mainActivity, false);
                    } else {
                        cardAdapter = new CardAdapter(songStack, mainActivity, true);
                    }
                    recyclerView.setAdapter(cardAdapter);
                    if (!showingSnackbar)
                        showToolbar();
                    return true;
                } else {
                    showFavs.setIcon(getDrawable(R.drawable.ic_favorite_red_24dp));
                    showingFavs = true;
                    cardAdapter = new CardAdapter(favoritesDataSource.getFavorites(),
                            mainActivity, false);
                    recyclerView.setAdapter(cardAdapter);
                    if (!showingSnackbar)
                        showToolbar();
                    return true;
                }
            }

        });
        MenuItem settings = (MenuItem) menu.findItem(R.id.action_settings);
        settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
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

    @Override
    public void onResume() {
        super.onResume();
        AudioPlayerBroadcastReceiver.setActivity(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        AudioPlayerBroadcastReceiver.setActivity(null);
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
            if(bound)
                localBinder.getService().sound();
            CardAdapter.playingTopCard = true;
            songStack.add(songInfo);
            cardAdapter.notifyItemInserted(0);
            cardAdapter.notifyItemChanged(1);
            if (lm.findFirstCompletelyVisibleItemPosition() == 0)
                recyclerView.smoothScrollToPosition(0);
        } else {
            if (sharedPreferences.getBoolean("muteAds", new Boolean(true))) {
                if (bound)
                    localBinder.getService().noSound();
            }
            CardAdapter.playingTopCard = false;
            cardAdapter.notifyItemChanged(0);
        }
        parser.execute();
    }

    private void startParser() {
        parser = new Parser(this);
        if (songStack.size() > 0) {
            SongInfo current = songStack.get(songStack.size() - 1);
            parser.songTitle = current.songName;
            parser.artistName = current.artistName;
        }
        parser.running = true;
        parser.execute();

    }

    private void stopParser() {
        parser.cancel(true);
        parser.running = false;
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopParser();
        favoritesDataSource.close();
        Log.d("91x", "DESTROYED!!!");
        unbindService(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
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

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d("91x", "Connection Established!");
        localBinder = (RadioService.LocalBinder) service;
        bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d("91x", "We Are Lost!");
        Intent intent = new Intent(getApplicationContext(), RadioService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
        bound = false;
    }

    @Override
    public void onClick(View v) {
        if (!bound)
            return;

        if (v.getTag().equals("play")) {
            localBinder.getService().play();
            streamPlaying();
        } else {
            localBinder.getService().stop();
            streamStopped();
        }
    }

    public void streamPlaying() {
        playPause.setTag("pause");
        playPause.setImageResource(R.drawable.ic_pause_circle_outline_black_36dp);
        cardAdapter = new CardAdapter(songStack, mainActivity, true);
        startParser();
    }

    public void streamStopped() {
        playPause.setTag("play");
        playPause.setImageResource(R.drawable.ic_play_circle_outline_black_36dp);
        cardAdapter = new CardAdapter(songStack, mainActivity, false);
        stopParser();
    }

    public void streamLoading() {
        progressBar.setIndeterminate(true);
    }

    public void streamLoaded() {
        progressBar.setIndeterminate(false);

    }


}