package org.stuartresearch.radio91x;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jake on 5/16/15.
 */
class FavoritesSqliteHelper extends SQLiteOpenHelper {
    public static final String COLUMN_ID = "trackId";
    public static final String COLUMN_SONGNAME = "songName";
    public static final String COLUMN_ARTISTNAME = "artistName";
    public static final String COLUMN_IMAGEURL = "imageUrl";
    public static final String COLUMN_BUYURL = "buyUrl";
    public static final String COLUMN_PREVIEWURL = "previewUrl";
    public static final String TABLE_NAME = "favs";

    private static final String DATABASE_NAME = "favorites.db";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE = String.format("CREATE TABLE %s (`%s` long," +
                    " `%s` varchar(50), `%s` varchar(50), `%s` varchar(500)," +
                    " `%s` varchar(500), `%s` varchar(500))", TABLE_NAME, COLUMN_ID,
            COLUMN_SONGNAME, COLUMN_ARTISTNAME, COLUMN_IMAGEURL, COLUMN_BUYURL, COLUMN_PREVIEWURL);

    public FavoritesSqliteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
