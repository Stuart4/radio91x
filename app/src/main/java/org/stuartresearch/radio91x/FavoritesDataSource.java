package org.stuartresearch.radio91x;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Vector;

/**
 * Created by jake on 5/16/15.
 */
public class FavoritesDataSource {
    private SQLiteDatabase db;
    private FavoritesSqliteHelper dbHelper;
    private String[] allColumns = {FavoritesSqliteHelper.COLUMN_ID,
            FavoritesSqliteHelper.COLUMN_SONGNAME, FavoritesSqliteHelper.COLUMN_ARTISTNAME,
            FavoritesSqliteHelper.COLUMN_IMAGEURL, FavoritesSqliteHelper.COLUMN_BUYURL,
            FavoritesSqliteHelper.COLUMN_PREVIEWURL};

    public FavoritesDataSource(Context context) {
        dbHelper = new FavoritesSqliteHelper(context);
    }

    public void open() throws SQLException {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void favorite(SongInfo songInfo) {
        ContentValues values = new ContentValues();
        values.put(FavoritesSqliteHelper.COLUMN_ID, songInfo.trackId);
        values.put(FavoritesSqliteHelper.COLUMN_SONGNAME, songInfo.songName);
        values.put(FavoritesSqliteHelper.COLUMN_ARTISTNAME, songInfo.artistName);
        values.put(FavoritesSqliteHelper.COLUMN_IMAGEURL, songInfo.imageUrl);
        values.put(FavoritesSqliteHelper.COLUMN_BUYURL, songInfo.buySong);
        values.put(FavoritesSqliteHelper.COLUMN_PREVIEWURL, songInfo.songSample);
        db.insert(FavoritesSqliteHelper.TABLE_NAME, null, values);
    }

    public void unfavorite(SongInfo songInfo) {
        db.delete(FavoritesSqliteHelper.TABLE_NAME,
                FavoritesSqliteHelper.COLUMN_ID + " = " + songInfo.trackId, null);
    }

    public Vector<SongInfo> getFavorites() {
        Cursor cursor = db.rawQuery("SELECT * FROM " + FavoritesSqliteHelper.TABLE_NAME, null);
        int size = cursor.getCount();
        SongInfo[] songInfos = new SongInfo[size];
        for (int i = 0; i < size; i++) {
            songInfos[i] = new SongInfo();
            cursor.moveToFirst();
            cursor.move(i);
            songInfos[i].trackId = cursor.getLong(0);
            cursor.moveToFirst();
            cursor.move(i);
            songInfos[i].songName = cursor.getString(1);
            cursor.moveToFirst();
            cursor.move(i);
            songInfos[i].artistName = cursor.getString(2);
            cursor.moveToFirst();
            cursor.move(i);
            songInfos[i].imageUrl = cursor.getString(3);
            cursor.moveToFirst();
            cursor.move(i);
            songInfos[i].buySong = cursor.getString(4);
            cursor.moveToFirst();
            cursor.move(i);
            songInfos[i].songSample = cursor.getString(5);
            cursor.moveToFirst();
            cursor.move(i);
            songInfos[i].favorite = true;
        }
        return new Vector<>(Arrays.asList(songInfos));
    }

    public boolean isFavorite (SongInfo songInfo) {
        return db.rawQuery("SELECT * FROM " + FavoritesSqliteHelper.TABLE_NAME
                + " WHERE " + FavoritesSqliteHelper.COLUMN_ID + " = " + songInfo.trackId, null)
                .getCount() > 0;
    }


}
