package org.stuartresearch.radio91x;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.internal.app.ToolbarActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toolbar;

import com.squareup.picasso.Picasso;

import java.util.Stack;

public class MainActivity extends ActionBarActivity {
    ImageView albumView;
    TextView songText;
    TextView artistText;
    Parser parser;
    Stack<SongInfo> songStack = new Stack<>();
    CardAdapter cardAdapter;

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
        this.songStack.push(songInfo);
        parser = new Parser(this);
        parser.currentSong = songInfo;
        parser.execute();
        songStack.push(songInfo);
        cardAdapter.notifyDataSetChanged();
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
