package com.tunashields.wand.data;

/**
 * Created by Irvin on 9/21/17.
 */

public interface IDeviceSchema {
    /* Table name */
    String TABLE_DEVICE = "device";
    /* Table column names */
    String ID = "id";
    String ADDRESS = "address";
    String NAME = "name";
    String OWNER = "owner";
    String PASSWORD = "password";
    String MODE = "mode";
    String RELAY = "relay";
    String VERSION = "version";
    String FIRMWARE = "firmware";
    String MANUFACTURING_DATE = "manufacturing_date";

    String CREATE_TABLE_DEVICE = "CREATE TABLE " + TABLE_DEVICE + "(" +
            ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            ADDRESS + " TEXT," +
            NAME + " TEXT," +
            OWNER + " TEXT," +
            PASSWORD + " TEXT," +
            MODE + " TEXT," +
            RELAY + " INTEGER," +
            VERSION + " TEXT," +
            FIRMWARE + " TEXT," +
            MANUFACTURING_DATE + " TEXT)";

    String[] DEVICE_COLUMNS = new String[]{ID, ADDRESS, NAME,
            OWNER, PASSWORD, MODE, RELAY, VERSION, FIRMWARE, MANUFACTURING_DATE};
}
