package com.tunashields.wand.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import com.tunashields.wand.models.WandDevice;
import com.tunashields.wand.utils.L;

import java.util.ArrayList;

/**
 * Created by Irvin on 9/21/17.
 */

public class WandDeviceDao extends DbContentProvider implements IDeviceSchema, IDeviceDao {

    private Cursor cursor;
    private ContentValues contentValues;

    public WandDeviceDao(SQLiteDatabase mSQLiteDatabase) {
        super(mSQLiteDatabase);
    }

    @Override
    public boolean addDevice(WandDevice device) {
        setContentValues(device);
        try {
            return super.insert(TABLE_DEVICE, getContentValues()) > 0;
        } catch (SQLiteConstraintException exception) {
            L.warning("Database: " + exception.getMessage());
            return false;
        }
    }

    @Override
    public WandDevice getDeviceById(int id) {
        String selectionArgs[] = {String.valueOf(id)};
        String selection = ID + " = ?";
        WandDevice device = new WandDevice();
        cursor = super.query(TABLE_DEVICE, DEVICE_COLUMNS, selection, selectionArgs, ID);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()){
                    device = cursorToEntity(cursor);
                    cursor.moveToNext();
                }
                cursor.close();
            }
        }
        return device;
    }

    @Override
    public ArrayList<WandDevice> getAllDevices() {
        ArrayList<WandDevice> devices = new ArrayList<>();
        cursor = super.query(TABLE_DEVICE, DEVICE_COLUMNS, null, null, ID);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    WandDevice device = cursorToEntity(cursor);
                    devices.add(device);
                    cursor.moveToNext();
                }
                cursor.close();
            }
        }
        return devices;
    }

    protected WandDevice cursorToEntity(Cursor cursor) {
        WandDevice device = new WandDevice();
        if (cursor != null) {
            device.id = cursor.getInt(cursor.getColumnIndex(ID));
            device.address = cursor.getString(cursor.getColumnIndex(ADDRESS));
            device.name = cursor.getString(cursor.getColumnIndex(NAME));
            device.owner = cursor.getString(cursor.getColumnIndex(OWNER));
            device.password = cursor.getString(cursor.getColumnIndex(PASSWORD));
            device.mode = cursor.getString(cursor.getColumnIndex(MODE));
            device.relay = cursor.getString(cursor.getColumnIndex(RELAY));
            device.version = cursor.getString(cursor.getColumnIndex(VERSION));
            device.manufacturing_date = cursor.getString(cursor.getColumnIndex(MANUFACTURING_DATE));
        }
        return device;
    }

    private void setContentValues(WandDevice device) {
        contentValues = new ContentValues();
        contentValues.put(ADDRESS, device.address);
        contentValues.put(NAME, device.name);
        contentValues.put(OWNER, device.owner);
        contentValues.put(PASSWORD, device.password);
        contentValues.put(MODE, device.mode);
        contentValues.put(RELAY, device.relay);
        contentValues.put(VERSION, device.version);
        contentValues.put(MANUFACTURING_DATE, device.manufacturing_date);
    }

    private ContentValues getContentValues() {
        return contentValues;
    }
}
