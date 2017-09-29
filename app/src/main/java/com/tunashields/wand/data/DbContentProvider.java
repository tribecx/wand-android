package com.tunashields.wand.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Irvin on 9/21/17.
 */

public abstract class DbContentProvider {
    public SQLiteDatabase mSQLiteDatabase;

    public DbContentProvider(SQLiteDatabase mSQLiteDatabase) {
        this.mSQLiteDatabase = mSQLiteDatabase;
    }

    public long insert(String tableName, ContentValues values) {
        return mSQLiteDatabase.insert(tableName, null, values);
    }

    public int update(String tableName, ContentValues values, String selection, String[] selectionArgs) {
        return mSQLiteDatabase.update(tableName, values, selection, selectionArgs);
    }

    public Cursor query(String tableName, String[] columns, String selection, String[] selectionArgs, String sortOrder) {
        return mSQLiteDatabase.query(tableName, columns, selection, selectionArgs, null, null, sortOrder);
    }
}