package com.tunashields.wand.bluetooth;

/**
 * Created by Irvin on 9/6/17.
 */

public class WandAttributes {
    /**
     * UUID's Used to communicate with the device
     */
    public static String WAND_ADVERTISEMENT_DATA_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static String WAND_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String WAND_CHARACTERISTIC = "0000ffe1-0000-1000-8000-00805f9b34fb";

    /**
     * UUID used to configure BLE notification responses
     */
    public static String CLIENT_CHARACTERISTIC_CONFIGURATION = "00002902-0000-1000-8000-00805f9b34fb";

    /**
     * Wand protocol default attributes
     */
    public static final String NEW_DEVICE_KEY = "Wand";
    public static final String CAR_DEFAULT_NAME = "Wand-Auto\r\n";
    public static final String DEFAULT_PASSWORD = "#P12345@";

    /**
     * Wand protocol responses
     */
    public static final String DETECT_NEW_CONNECTION = "#P@";
    public static final String DEFAULT_PASSWORD_OK = "#P:OK@";
    public static final String DEFAULT_PASSWORD_ERROR = "#P:NO@";
    public static final String NAME_OK = "#N:OK@";
    public static final String PASSWORD_OK = "#C:OK@";
    public static final String PASSWORD_NO = "#C:NO@";
}