package com.tunashields.wand.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Irvin on 9/21/17.
 */

public abstract class DbContentProvider {
    public SQLiteDatabase mSQLiteDatabase;

    public int delete(String tableName, String selection,
                      String[] selectionArgs) {
        return mSQLiteDatabase.delete(tableName, selection, selectionArgs);
    }

    public long insert(String tableName, ContentValues values) {
        return mSQLiteDatabase.insert(tableName, null, values);
    }

    public DbContentProvider(SQLiteDatabase mSQLiteDatabase) {
        this.mSQLiteDatabase = mSQLiteDatabase;
    }

    public Cursor query(String tableName, String[] columns,
                        String selection, String[] selectionArgs, String sortOrder) {

        final Cursor cursor = mSQLiteDatabase.query(tableName, columns,
                selection, selectionArgs, null, null, sortOrder);

        return cursor;
    }

    public Cursor query(String tableName, String[] columns,
                        String selection, String[] selectionArgs, String sortOrder,
                        String limit) {

        return mSQLiteDatabase.query(tableName, columns, selection,
                selectionArgs, null, null, sortOrder, limit);
    }

    public Cursor query(String tableName, String[] columns,
                        String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy, String limit) {

        return mSQLiteDatabase.query(tableName, columns, selection,
                selectionArgs, groupBy, having, orderBy, limit);
    }

    public int update(String tableName, ContentValues values,
                      String selection, String[] selectionArgs) {
        return mSQLiteDatabase.update(tableName, values, selection,
                selectionArgs);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return mSQLiteDatabase.rawQuery(sql, selectionArgs);
    }
}