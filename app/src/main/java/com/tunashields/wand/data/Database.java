package com.tunashields.wand.data;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.tunashields.wand.utils.L;

/**
 * Created by Irvin on 9/20/17.
 */

public class Database {
    /* Database version*/
    private static final int DATABASE_VERSION = 1;
    /* Database name */
    private static final String DATABASE_NAME = "wand_database.s3db";

    private DatabaseHelper mDatabaseHelper;

    private Context mContext;

    public static WandDeviceDao mWandDeviceDao;

    public Database(Context mContext) {
        this.mContext = mContext;
    }

    public Database open() throws SQLException {
        mDatabaseHelper = new DatabaseHelper(mContext);
        SQLiteDatabase mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
        mWandDeviceDao = new WandDeviceDao(mSQLiteDatabase);
        return this;
    }

    public void close() {
        mDatabaseHelper.close();
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(IDeviceSchema.CREATE_TABLE_DEVICE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            L.warning("Upgrading database from version "
                    + oldVersion + " to "
                    + newVersion + " which destroys all old data");
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + IDeviceSchema.TABLE_DEVICE);
            onCreate(sqLiteDatabase);
        }
    }
}
