package com.huxq17.download.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.huxq17.download.provider.Provider;

public class DBOpenHelper extends SQLiteOpenHelper {
    public DBOpenHelper(Context context) {
        super(context, "pump.db", null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Provider.DownloadTable.TABLE_NAME + " ("
                + Provider.DownloadTable.URL + " CHAR,"
                + Provider.DownloadTable.PATH + " CHAR,"
                + Provider.DownloadTable.THREAD_NUM + " INTEGER,"
                + Provider.DownloadTable.FILE_LENGTH + " INTEGER,"
                + Provider.DownloadTable.FINISHED + " INTEGER,"
                + Provider.DownloadTable.CREATE_TIME + " INTEGER,"
                + Provider.DownloadTable.TAG + " CHAR,"
                + "primary key(" + Provider.DownloadTable.URL + "," + Provider.DownloadTable.PATH + ")"
                + ");");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Provider.CacheTable.TABLE_NAME + " ("
                + Provider.CacheTable.URL + " CHAR primary key,"
                + Provider.CacheTable.ETAG + " CHAR,"
                + Provider.CacheTable.LAST_MODIFIED + " CHAR"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            onCreate(db);
        } else if (newVersion == 3) {
            db.execSQL("ALTER TABLE " + Provider.DownloadTable.TABLE_NAME + " ADD COLUMN " + Provider.DownloadTable.TAG + " CHAR default('');");
            onCreate(db);
        }
    }

}