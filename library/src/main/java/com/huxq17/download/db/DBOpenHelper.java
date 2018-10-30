package com.huxq17.download.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.huxq17.download.provider.Provider;

public class DBOpenHelper extends SQLiteOpenHelper {
    public DBOpenHelper(Context context) {
        super(context, "pump.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Provider.DownloadInfo.TABLE_NAME + " ("
                + Provider.DownloadInfo.URL + " CHAR,"
                + Provider.DownloadInfo.PATH + " CHAR,"
                + Provider.DownloadInfo.THREAD_NUM + " INTEGER,"
                + Provider.DownloadInfo.FILE_LENGTH + " INTEGER,"
                + Provider.DownloadInfo.FINISHED + " INTEGER,"
                + Provider.DownloadInfo.CREATE_TIME + " INTEGER,"
                + "primary key(" + Provider.DownloadInfo.URL + "," + Provider.DownloadInfo.PATH + ")"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO 当版本号发生改变时调用该方法,这里删除数据表,在实际业务中一般是要进行数据备份的
        db.execSQL("DROP TABLE IF EXISTS " + Provider.DownloadInfo.TABLE_NAME);
        onCreate(db);
    }

}