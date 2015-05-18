package org.stuartresearch.radio91x;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;


/**
 * Created by jake on 5/13/15.
 */
class Parser extends AsyncTask<Void, Void, SongInfo> {
    boolean running = true;

    private final MainActivity main;
    String songTitle = new String();
    String artistName = new String();

    public Parser(MainActivity mainActivity) {
        main = mainActivity;
    }

    @Override
    protected SongInfo doInBackground(Void... params) {
        while (running) {
            try {

                URL url = new URL("http://playerservices.streamtheworld.com" +
                        "/api/livestream-redirect/XTRAFM.mp3");
                ParsingHeaderData streaming = new ParsingHeaderData();
                ParsingHeaderData.TrackData trackData = streaming.getTrackDetails(url);
                Log.d("91X", String.format("Read -> Artist: %s, Title: %s", trackData.artist,
                        trackData.title));
                if (!songTitle.equals(trackData.title)) {
                    if (!artistName.equals(trackData.artist)) {
                        songTitle = trackData.title;
                        artistName = trackData.artist;
                        SongInfo songInfo = new SongInfo();
                        if (songTitle.isEmpty() && artistName.isEmpty()) {
                            songInfo.trackId = -666;
                            songInfo.songName = main.getString(R.string.advertisement);
                            return songInfo;
                        }
                        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
                        String jsonUrl = String.format("https://itunes.apple.com/search?term=" +
                                        "%s+%s&country=us&callback=results&limit=1&entity=song",
                                songTitle.replace(" ", "+"), artistName.replace(" ", "+"));
                        HttpGet httpGet = new HttpGet(jsonUrl);
                        try {
                            HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
                            BufferedReader reader =
                                    new BufferedReader(new InputStreamReader(httpResponse.
                                            getEntity().getContent(), "UTF-8"));
                            String line;
                            String json = new String();
                            reader.readLine();
                            reader.readLine();
                            reader.readLine();

                            while ((line = reader.readLine()) != null) {
                                json += line;
                            }
                            json = json.substring(0, json.length() - 2);
                            JSONObject jsonObject = new JSONObject(json);
                            if (jsonObject.getInt("resultCount") == 0) {
                                songInfo.songName = songTitle;
                                songInfo.artistName = artistName;
                                return songInfo;
                            }
                            JSONArray jsonArray = jsonObject.getJSONArray("results");
                            jsonObject = jsonArray.getJSONObject(0);
                            try {
                                songInfo.imageUrl = jsonObject.getString("artworkUrl100")
                                        .replaceFirst("100x100-75.jpg$", "600x600-50.jpg");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                songInfo.songSample = jsonObject.getString("previewUrl");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                songInfo.songName = jsonObject.getString("trackName");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                songInfo.artistName = jsonObject.getString("artistName");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                songInfo.buySong = jsonObject.getString("trackViewUrl");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            try {
                                songInfo.trackId = jsonObject.getLong("trackId");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return songInfo;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(30000);
            } catch (InterruptedException dropIt) {}
        }
        return null;
    }

    @Override
    protected void onPostExecute(SongInfo songInfo) {
        if (songInfo == null) {
            new Parser(main).execute();
            return;
        }
        main.updateSongInfo(songInfo);

    }
}

class SongInfo implements Serializable {
    String songName = new String();
    String artistName = new String();
    String imageUrl = new String();
    long trackId = 0;
    String buySong = new String();
    String songSample = new String();
    boolean favorite = false;

    public String toString() {
        return songName + ":" + artistName + ":" + imageUrl;
    }

    public boolean equals(SongInfo song) {
        return song.trackId == trackId;
    }
}


