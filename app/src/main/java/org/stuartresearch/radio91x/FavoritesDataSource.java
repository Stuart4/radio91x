package org.stuartresearch.radio91x;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by jake on 5/16/15.
 */
class FavoritesDataSource {
    private SQLiteDatabase db;
    final private FavoritesSqliteHelper dbHelper;
    /*private String[] allColumns = {FavoritesSqliteHelper.COLUMN_ID,
            FavoritesSqliteHelper.COLUMN_SONGNAME, FavoritesSqliteHelper.COLUMN_ARTISTNAME,
            FavoritesSqliteHelper.COLUMN_IMAGEURL, FavoritesSqliteHelper.COLUMN_BUYURL,
            FavoritesSqliteHelper.COLUMN_PREVIEWURL};*/

    public FavoritesDataSource(Context context) {
        dbHelper = new FavoritesSqliteHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void favorite(SongInfo songInfo) {
        if (!db.isOpen()) return;
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
        if (!db.isOpen()) return;
        db.delete(FavoritesSqliteHelper.TABLE_NAME,
                FavoritesSqliteHelper.COLUMN_ID + " = " + songInfo.trackId, null);
    }

    public SongStack getFavorites() {
        if(!db.isOpen()) return new SongStack(1);
        Cursor cursor = db.rawQuery("SELECT * FROM " + FavoritesSqliteHelper.TABLE_NAME, null);
        int size = cursor.getCount();
        SongStack songInfos = new SongStack(size);
        for (int i = 0; i < size; i++) {
            SongInfo si = new SongInfo();
            cursor.moveToFirst();
            cursor.move(i);
            si.trackId = cursor.getLong(0);
            cursor.moveToFirst();
            cursor.move(i);
            si.songName = cursor.getString(1);
            cursor.moveToFirst();
            cursor.move(i);
            si.artistName = cursor.getString(2);
            cursor.moveToFirst();
            cursor.move(i);
            si.imageUrl = cursor.getString(3);
            cursor.moveToFirst();
            cursor.move(i);
            si.buySong = cursor.getString(4);
            cursor.moveToFirst();
            cursor.move(i);
            si.songSample = cursor.getString(5);
            cursor.moveToFirst();
            cursor.move(i);
            si.favorite = true;
            songInfos.insert(si);
        }
        cursor.close();
        return songInfos;
    }

    public boolean isFavorite(SongInfo songInfo) {
        if(!db.isOpen()) return false;
        return db.rawQuery("SELECT * FROM " + FavoritesSqliteHelper.TABLE_NAME
                + " WHERE " + FavoritesSqliteHelper.COLUMN_ID + " = " + songInfo.trackId, null)
                .getCount() > 0;
    }


}
