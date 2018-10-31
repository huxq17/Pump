package com.huxq17.download.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.db.DBService;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.IMessageCenter;


public class DownloadProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DBService.init(context);
        ServiceAgency.getService(IMessageCenter.class).start(context);
        ServiceAgency.getService(IDownloadManager.class).start(context);
        //If DownloadService is running,pause it.
//        Intent intent = new Intent(context, DownloadService.class);
//        context.stopService(intent);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri,
                        @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        throw new SQLException("Not support to query.");
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new SQLException("Not support to insert.");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new SQLException("Not support to delete.");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new SQLException("Not support to update.");
    }

    private SQLiteDatabase getDatabase() {
        return DBService.getInstance().getReadableDatabase();
    }
}
