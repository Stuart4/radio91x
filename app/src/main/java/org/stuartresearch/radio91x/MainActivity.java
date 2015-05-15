package org.stuartresearch.radio91x;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.squareup.picasso.Picasso;

import java.util.Stack;

public class MainActivity extends ActionBarActivity {
    ImageView albumView;
    TextView songText;
    TextView artistText;
    Parser parser;
    Stack<SongInfo> songStack = new Stack<>();
    CardAdapter cardAdapter;
    boolean toolbarShowing = true;
    RecyclerView recyclerView;
    boolean showingFavs = false;
    Streamer streamer;
    AudioManager.OnAudioFocusChangeListener afChangeListener;
    AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ImageView playPause = (ImageView) findViewById(R.id.controlImageView);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    if (streamer.isPlaying()) playPause.callOnClick();
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
                } else {
                    int res = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) return;
                    streamer.play();
                    showNotification();
                    startParser();
                    playPause.setImageResource(R.drawable.ic_pause_black_18dp);
                }
            }
        });
        if (res != AudioManager.AUDIOFOCUS_REQUEST_FAILED)
            startParser();
        recyclerView = (RecyclerView) findViewById(R.id.cardList);
        recyclerView.setHasFixedSize(false);
        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(lm);
        cardAdapter = new CardAdapter(songStack, this);
        recyclerView.setAdapter(cardAdapter);
        if (Build.VERSION.SDK_INT >20)
            getWindow().setNavigationBarColor(getResources().getColor(R.color.primary_dark));
        final android.support.v7.widget.Toolbar toolBar =
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
                if (scrollState == ScrollState.UP && toolbarShowing) {
                    toolBar.animate().translationY(toolBar.getHeight()).setInterpolator(new AccelerateInterpolator()).start();
                    toolbarShowing = false;

                } else if (scrollState == ScrollState.DOWN && !toolbarShowing) {
                    toolBar.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
                    toolbarShowing = true;
                }
            }
        });

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
                    intent.setData(Uri.parse("tel:5701919"));
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
                    return true;
                } else {
                    showFavs.setIcon(getResources().getDrawable(R.drawable.ic_favorite_red_18dp));
                    showingFavs = true;
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
            return;
        }
        if (songStack.size() >= 2 && songInfo.trackId == songStack.peek().trackId) {
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
                    .placeholder(getResources().getDrawable(R.drawable.notes_background))
                    .into(albumView);
            albumView.setVisibility(View.VISIBLE);
        } else {
            albumView.setVisibility(View.GONE);
        }
        parser = new Parser(this);
        parser.songTitle = songInfo.songName;
        parser.artistName = songInfo.artistName;
        if (songInfo.trackId != -666) {
            songStack.push(songInfo);
            cardAdapter.notifyItemInserted(0);
            cardAdapter.notifyItemChanged(1);
            if (recyclerView.getScrollY() == 0)
                recyclerView.smoothScrollToPosition(0);
        }
        parser.execute();
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
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_headset_black_18dp)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigPictureStyle())
                .setContentTitle("91x")
                .setContentText(String.format("You are listening to %s by %s on San Diego's 91x.",
                        songText.getText(), artistText.getText()));
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
        notificationManager.notify(919191, notification);

    }

    public void hideNotification() {

    }

    @Override
    protected void onStart() {
        super.onStart();
        showNotification();
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
    }

    @Override
    protected void onNewIntent(Intent intent) {

    }


}
