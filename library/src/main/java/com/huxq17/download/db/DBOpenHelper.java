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
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Provider.DownloadTable.TABLE_NAME + " ("
                + Provider.DownloadTable.URL + " CHAR,"
                + Provider.DownloadTable.PATH + " CHAR,"
                + Provider.DownloadTable.THREAD_NUM + " INTEGER,"
                + Provider.DownloadTable.FILE_LENGTH + " INTEGER,"
                + Provider.DownloadTable.FINISHED + " INTEGER,"
                + Provider.DownloadTable.CREATE_TIME + " INTEGER,"
                + "primary key(" + Provider.DownloadTable.URL + "," + Provider.DownloadTable.PATH + ")"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO 当版本号发生改变时调用该方法,这里删除数据表,在实际业务中一般是要进行数据备份的
        db.execSQL("DROP TABLE IF EXISTS " + Provider.DownloadTable.TABLE_NAME);
        onCreate(db);
    }

}