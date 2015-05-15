package org.stuartresearch.radio91x;

import android.os.AsyncTask;
import android.util.Log;

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
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by jake on 5/13/15.
 */
public class Parser extends AsyncTask<Void, Void, SongInfo> {
    boolean running = true;
    private SAXParserFactory spf;
    MainActivity main;
    String songTitle = new String();
    String artistName = new String();

    public Parser(MainActivity mainActivity) {
        spf = SAXParserFactory.newInstance();
        main = mainActivity;
    }

    @Override
    protected SongInfo doInBackground(Void... params) {
        while (running) {
            try {

                URL url = new URL("http://playerservices.streamtheworld.com/api/livestream-redirect/XTRAFM.mp3");
                ParsingHeaderData streaming = new ParsingHeaderData();
                ParsingHeaderData.TrackData trackData = streaming.getTrackDetails(url);
                Log.d("91X", String.format("Read -> Artist: %s, Title: %s", trackData.artist, trackData.title));
                if (!songTitle.equals(trackData.title)) {
                    if (!artistName.equals(trackData.artist)) {
                        SAXParser sp = spf.newSAXParser();
                        DefaultHand hand = new DefaultHand();
                        sp.parse("http://np.tritondigital.com/public/nowplaying?mountName=XTRAFM&numberToFetch=1&random=" + System.currentTimeMillis(), hand);
                        if (hand.songInfo.artistName.equals(trackData.artist) && hand.songInfo.songName.equals(trackData.title)) {
                            songTitle = trackData.title;
                            artistName = trackData.artist;
                            if (hand.songInfo.trackId == -666) {
                                hand.songInfo.songName = "Advertisement";
                                return hand.songInfo;
                            }
                            DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
                            HttpGet httpGet = new HttpGet(hand.songInfo.jsonUrl);
                            try {
                                HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
                                String line;
                                String json = new String();
                                while ((line = reader.readLine()) != null) {
                                    json += line;
                                }
                                JSONObject jsonObject = new JSONObject(json);
                                hand.songInfo.imageUrl = jsonObject.getJSONObject("song").getJSONObject("album").getJSONObject("cover").getString("originalSourceUrl");
                                hand.songInfo.songSample = jsonObject.getJSONObject("song").getJSONObject("track").getString("sampleURL");
                                hand.songInfo.buySong = jsonObject.getJSONObject("song").getJSONObject("track").getString("buyURL");
                                return hand.songInfo;
                            } catch (Exception e) {
                                return hand.songInfo;
                            }
                        }
                        //stream data and xml do not match
                        else {
                            hand.songInfo.trackId = -666;
                            hand.songInfo.songName = "Out of Sync";
                            hand.songInfo.artistName = "";
                            return hand.songInfo;
                        }
                    }
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(8000);
            } catch (InterruptedException e) {}
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
            songInfo.songName = new String(ch).substring(start, length);
            return;
        } else if (artist) {
            songInfo.artistName = new String(ch).substring(start, length);
            return;
        } else if (url) {
            songInfo.jsonUrl = new String(ch).substring(start, length);
            return;
        } else if (id) {
            songInfo.trackId = Integer.valueOf(new String(ch).substring(start, length));
            return;
        }
    }
}


