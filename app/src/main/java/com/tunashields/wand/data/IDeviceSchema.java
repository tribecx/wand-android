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
    String SERIAL_NUMBER = "serial_number";
    String VERSION = "version";
    String MANUFACTURING_DATE = "manufacturing_date";

    String CREATE_TABLE_DEVICE = "CREATE TABLE " + TABLE_DEVICE + "(" +
            ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            ADDRESS + " TEXT," +
            NAME + " TEXT," +
            OWNER + " TEXT," +
            PASSWORD + " TEXT," +
            MODE + " TEXT," +
            RELAY + " INTEGER," +
            SERIAL_NUMBER + " TEXT," +
            VERSION + " TEXT," +
            MANUFACTURING_DATE + " TEXT)";

    String[] DEVICE_COLUMNS = new String[]{ID, ADDRESS, NAME,
            OWNER, PASSWORD, MODE, RELAY, SERIAL_NUMBER, VERSION, MANUFACTURING_DATE};
}
