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
     * UUID used to configure BLE notifications
     */
    public static String CLIENT_CHARACTERISTIC_CONFIGURATION = "00002902-0000-1000-8000-00805f9b34fb";

    /**
     * Wand protocol default attributes
     */
    public static final String NEW_DEVICE_KEY = "Wand";
    public static final String CAR_DEFAULT_NAME = "Wand-Auto\r\n";
    public static final String DEFAULT_PASSWORD = "12345";

    /**
     * Wand protocol responses
     */
    public static final String DETECT_NEW_CONNECTION = "#P@";
    public static final String ENTER_PASSWORD_OK = "#P:OK@";
    public static final String ENTER_PASSWORD_ERROR = "#P:NO@";
    public static final String CHANGE_NAME_OK = "#N:OK@";
    public static final String CHANGE_PASSWORD_OK = "#C:OK@";
    public static final String CHANGE_PASSWORD_ERROR = "#C:NO@";
    public static final String ENABLE_RELAY_OK = "#R1:OK@";
    public static final String ENABLE_RELAY_ERROR = "#R1:NO@";
    public static final String DISABLE_RELAY_OK = "#R0:OK@";
    public static final String DISABLE_RELAY_ERROR = "#R0:NO@";
    public static final String AUTOMATIC_MODE_OK = "#MA:OK@";
    public static final String MANUAL_MODE_OK = "#MM:OK@";

    public static final String MODE_MANUAL = "M=M";
    public static final String MODE_AUTOMATIC = "M=A";
    public static final String RELAY_ENABLED = "R=1";
    public static final String RELAY_DISABLED = "R=0";
}