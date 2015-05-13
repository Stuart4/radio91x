package org.stuartresearch.radio91x;

import android.content.Context;
import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by jake on 5/13/15.
 */
public class Parser extends AsyncTask<Void, Void, SongInfo> {
    boolean running = true;
    SongInfo currentSong;
    private SAXParserFactory spf;
    MainActivity main;

    public Parser(MainActivity mainActivity) {
        spf = SAXParserFactory.newInstance();
        main = mainActivity;
    }

    @Override
    protected SongInfo doInBackground(Void... params) {
        while (running) {
            try {
                SAXParser sp = spf.newSAXParser();
                DefaultHand hand = new DefaultHand();
                sp.parse("http://np.tritondigital.com/public/nowplaying?mountName=XTRAFM&numberToFetch=1&random=" + System.currentTimeMillis() / 1000L , hand);
                if (currentSong == null || !currentSong.equals(hand.songInfo)) {
                    currentSong = hand.songInfo;
                    return currentSong;
                } else {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(SongInfo songInfo) {
        if (songInfo.trackId == -666) {
            songInfo.songName = "Advertisement";
            main.updateSongInfo(songInfo);
        }
        new JParser(main).execute(songInfo);
    }
}

class SongInfo {
    String songName = new String();
    String artistName = new String();
    String imageUrl = new String();
    String jsonUrl = new String();
    int trackId = 0;
    String buySong = new String();
    String songSample = new String();

    public String toString() {
        return songName + ":" + artistName + ":" + imageUrl;
    }

    public boolean equals(SongInfo song) {
        return song.trackId == trackId;
    }
}

class DefaultHand extends DefaultHandler {
    boolean song = false;
    boolean artist = false;
    boolean url = false;
    boolean id = false;
    boolean ad = false;
    SongInfo songInfo = new SongInfo();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.compareTo("property") == 0 && !ad) {
            switch (attributes.getValue(0)) {
                case "ad_id":
                    ad = true;
                    return;
                case "cue_title":
                    song = true;
                    break;
                case "track_artist_name":
                    artist = true;
                    break;
                case "track_nowplaying_url":
                    url = true;
                    break;
                case "track_id":
                    id = true;
                    break;
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.compareTo("property") == 0) {
            song = false;
            artist = false;
            url = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (ad) {
            songInfo.trackId = -666;
            return;
        } else if (song) {
            songInfo.songName = new String(ch).substring(0, length);
            return;
        } else if (artist) {
            songInfo.artistName = new String(ch).substring(0, length);
            return;
        } else if (url) {
            songInfo.jsonUrl = new String(ch).substring(0, length);
            return;
        } else if (id) {
            songInfo.trackId = Integer.valueOf(new String(ch).substring(0, length));
            return;
        }
    }
}

class JParser extends AsyncTask<SongInfo, Void, SongInfo> {
    MainActivity main;

    public JParser(MainActivity con) {
        main = con;
    }
    @Override
    protected SongInfo doInBackground(SongInfo... params) {
        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(params[0].jsonUrl);
        try {
            HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
            String line;
            String json = new String();
            while ((line = reader.readLine()) != null) {
                json += line;
            }
            JSONObject jsonObject = new JSONObject(json);
            params[0].imageUrl = jsonObject.getJSONObject("song").getJSONObject("album").getJSONObject("cover").getString("originalSourceUrl");
            params[0].songSample = jsonObject.getJSONObject("song").getJSONObject("track").getString("sampleUrl");
            params[0].buySong = jsonObject.getJSONObject("song").getJSONObject("track").getString("buyURL");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params[0];
    }

    @Override
    protected void onPostExecute(SongInfo songInfo) {
        main.updateSongInfo(songInfo);
    }
}
