package org.stuartresearch.radio91x;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.internal.app.ToolbarActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toolbar;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ImageView playPause = (ImageView) findViewById(R.id.controlImageView);
        final Streamer streamer = new Streamer(getApplicationContext(), (ProgressBar) findViewById(R.id.progressBar), playPause);
        albumView = (ImageView) findViewById(R.id.albumImageView);
        songText = (TextView) findViewById(R.id.songNameTextView);
        artistText = (TextView) findViewById(R.id.ArtistNameTextView);
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (streamer.isPlaying()) {
                    streamer.stop();
                    stopParser();
                    playPause.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    streamer.play();
                    startParser();
                    playPause.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        });
        startParser();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.cardList);
        recyclerView.setHasFixedSize(false);
        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(lm);
        cardAdapter = new CardAdapter(songStack);
        recyclerView.setAdapter(cardAdapter);
        if (Build.VERSION.SDK_INT >20)
            getWindow().setNavigationBarColor(getResources().getColor(R.color.primary_dark));
        final android.support.v7.widget.Toolbar toolBar = (android.support.v7.widget.Toolbar) findViewById(R.id.lowerToolbar);
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
        if (songInfo == null) {
            parser = new Parser(this);
            parser.currentSong = null;
            parser.execute();
            return;
        }
        songText.setText(songInfo.songName);
        artistText.setText(songInfo.artistName);
        if (songInfo.imageUrl.length() > 0) {
            Picasso.with(this).load(songInfo.imageUrl).into(albumView);
            albumView.setVisibility(View.VISIBLE);
        }
        parser = new Parser(this);
        parser.currentSong = songInfo;
        parser.execute();
        if (songInfo.trackId != -666) {
            songStack.push(songInfo);
            //cardAdapter.notifyDataSetChanged();
            cardAdapter.notifyItemInserted(0);
        }
    }

    public void startParser() {
        parser = new Parser(this);
        parser.execute();
        parser.running = true;

    }

    public void stopParser() {
        parser.running = false;
    }


}
