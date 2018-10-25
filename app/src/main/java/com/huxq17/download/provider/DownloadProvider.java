package com.huxq17.download.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.ContextHelper;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.IDownloadObserverManager;
import com.huxq17.download.message.IMessageCenter;


public class DownloadProvider extends ContentProvider {
    private static final UriMatcher sUriMatcher;

    private static final int ID_BLOCK = 1;
    private static final int ID_INFO = 2;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Provider.AUTHORITY, "block", ID_BLOCK);
    }


    @Override
    public boolean onCreate() {
        Context context = getContext();
        DBService.init(context);
        ContextHelper.init(context);
        ServiceAgency.getService(IMessageCenter.class).start(context);
        ServiceAgency.getService(IDownloadObserverManager.class).start(context);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri,
                        @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        MatrixCursor cursor;
        String table = getTableName(uri);
        Cursor result = getDatabase().query(table, projection, selection, selectionArgs, null, null, sortOrder, null);
        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
//        String table = getTableName(uri);
//        long id = getDatabase().insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE);
//        if (id >= 0) {
//            Uri result = ContentUris.appendId(uri.buildUpon(), id).build();
//            getContext().getContentResolver().notifyChange(uri, null);
//            return result;
//        } else {
//            throw new SQLException("Fail to insert to row :" + uri);
//        }
        throw new SQLException("Not support to insert.");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
//        String table = getTableName(uri);
//        int count = getDatabase().delete(table, selection, selectionArgs);
//        getContext().getContentResolver().notifyChange(uri, null);
//        return count;
        throw new SQLException("Not support to delete.");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
//        String table = getTableName(uri);
//        int count = getDatabase().update(table, values, selection, selectionArgs);
//        getContext().getContentResolver().notifyChange(uri, null);
//        return count;
        throw new SQLException("Not support to update.");
    }

    private String getTableName(Uri uri) {
        String tableName = null;
        switch (sUriMatcher.match(uri)) {
            case ID_BLOCK:
                tableName = Provider.DownloadBlock.TABLE_NAME;
                break;
            case ID_INFO:
                tableName = Provider.DownloadInfo.TABLE_NAME;
                break;
        }
        return tableName;
    }

    private SQLiteDatabase getDatabase() {
        return DBService.getInstance().getReadableDatabase();
    }
}
