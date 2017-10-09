package com.tunashields.wand.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.tunashields.wand.data.Database;
import com.tunashields.wand.models.WandDevice;
import com.tunashields.wand.utils.L;
import com.tunashields.wand.utils.WandUtils;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Irvin on 9/12/17.
 */

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    //private String mBluetoothDeviceAddress;
    //private BluetoothGatt mBluetoothGatt;
    private HashMap<String, BluetoothGatt> mGattHashMap;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.tunashields.wand.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.tunashields.wand.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.tunashields.wand.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.tunashields.wand.ACTION_DATA_AVAILABLE";
    public final static String ACTION_DEVICE_CONNECTED =
            "com.tunashields.wand.ACTION_DEVICE_CONNECTED"; // TODO: 10/5/17 Update field with name 'ACTION_PASSWORD_ENTERED' or something like that
    public final static String EXTRA_DATA =
            "com.tunashields.wand.EXTRA_DATA";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                L.info("Connected to GATT server.");
                // Attempts to discover services after successful connection.
                L.info("Attempting to start service discovery: " + mGattHashMap.get(gatt.getDevice().getAddress()).discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                L.info("Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                L.warning("onServicesDiscovered received: " + status);
            }

            if (mBluetoothAdapter == null || mGattHashMap.get(gatt.getDevice().getAddress()) == null) {
                L.warning("BluetoothAdapter not initialized");
                return;
            }

            BluetoothGattService mCustomService = gatt.getService(UUID.fromString(WandAttributes.WAND_SERVICE));
            if (mCustomService != null) {
                BluetoothGattCharacteristic characteristic = mCustomService.getCharacteristic(UUID.fromString(WandAttributes.WAND_CHARACTERISTIC));
                mGattHashMap.get(gatt.getDevice().getAddress()).setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(WandAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mGattHashMap.get(gatt.getDevice().getAddress()).writeDescriptor(descriptor);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            byte[] value = characteristic.getValue();
            String v = new String(value);
            L.info("onCharacteristicChanged: Value = " + v);

            switch (v) {
                case WandAttributes.DETECT_NEW_CONNECTION:
                    WandDevice wandDevice = Database.mWandDeviceDao.getDeviceByAddress(gatt.getDevice().getAddress());
                    if (wandDevice != null)
                        writeCharacteristic(gatt.getDevice().getAddress(), WandUtils.setEnterPasswordFormat(wandDevice.password));

                    broadcastUpdate(ACTION_DEVICE_CONNECTED, gatt.getDevice().getAddress());
                    break;
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        intent.putExtra(EXTRA_DATA, new String(data));
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String address) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, address);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        closeGattConnections();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                L.error("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            L.error("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        if (mGattHashMap == null)
            mGattHashMap = new HashMap<>();

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            L.warning("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mGattHashMap.containsKey(address)) {
            if (mGattHashMap.get(address) != null) {
                L.debug("Trying to use an existing mBluetoothGatt for connection.");
                if (mGattHashMap.get(address).connect()) {
                    mConnectionState = STATE_CONNECTING;
                    return true;
                } else {
                    return false;
                }
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            L.warning("Device not found.  Unable to connect.");
            return false;
        }

        // We want to connect automatically to the device, so we are setting the autoConnect
        // parameter to true.
        mGattHashMap.put(address, device.connectGatt(this, true, mGattCallback));

        L.debug("Trying to create a new connection.");
        //mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(String address) {
        if (mGattHashMap.containsKey(address)) {
            if (mBluetoothAdapter == null || mGattHashMap.get(address) == null) {
                L.warning("BluetoothAdapter not initialized");
                return;
            }
            mGattHashMap.get(address).disconnect();
            mGattHashMap.remove(address);
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public boolean writeCharacteristic(String address, String value) {
        if (mBluetoothAdapter == null || mGattHashMap.get(address) == null) {
            L.warning("BluetoothAdapter not initialized");
            return false;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mService = mGattHashMap.get(address).getService(UUID.fromString(WandAttributes.WAND_SERVICE));
        if (mService == null) {
            L.warning("Wand BLE Service not found");
            return false;
        }
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mCharacteristic = mService.getCharacteristic(UUID.fromString(WandAttributes.WAND_CHARACTERISTIC));
        if (mCharacteristic == null) {
            L.warning("Wand BLE Characteristic not found");
            return false;
        }

        mCharacteristic.setValue(value);

        if (!mGattHashMap.get(address).writeCharacteristic(mCharacteristic)) {
            L.warning("Failed to write characteristic");
            return false;
        } else {
            return true;
        }
    }

    public void closeGattConnections() {
        for (WandDevice wandDevice : Database.mWandDeviceDao.getAllDevices()) {
            if (mGattHashMap.containsKey(wandDevice.address)) {
                mGattHashMap.get(wandDevice.address).close();
            }
        }
        mGattHashMap = null;
    }
}
