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
import android.os.Build;
import android.os.IBinder;

import com.tunashields.wand.utils.L;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
    public HashMap<String, BluetoothGatt> mGattHashMap;
    public ArrayList<String> mConnectedAddresses;

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

    public final static String EXTRA_DEVICE_ADDRESS =
            "com.tunashields.wand.EXTRA_DEVICE_ADDRESS";
    public final static String EXTRA_DATA =
            "com.tunashields.wand.EXTRA_DATA";

    public final static String ERROR_CONFIGURATION =
            "com.tunashields.wand.ERROR_CONFIGURATION";

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
                if (mGattHashMap.containsKey(gatt.getDevice().getAddress())) {
                    mGattHashMap.get(gatt.getDevice().getAddress()).discoverServices();
                    L.info("Attempting to start service discovery: " + mGattHashMap.get(gatt.getDevice().getAddress()).discoverServices());
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                L.info("Disconnected from GATT server.");
                broadcastUpdate(intentAction, gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                L.debug("Discover services status: " + status);

                String address = gatt.getDevice().getAddress();

                /**
                 * Looking for Wand BLE Service.
                 * */
                BluetoothGattService mCustomService = mGattHashMap.get(address).getService(UUID.fromString(WandAttributes.WAND_SERVICE));
                if (mCustomService != null) {
                    /**
                     * Subscribing Wand BLE Characteristic to notifications.
                     * */
                    BluetoothGattCharacteristic mCustomCharacteristic = mCustomService.getCharacteristic(UUID.fromString(WandAttributes.WAND_CHARACTERISTIC));
                    if (mCustomCharacteristic != null) {
                        //mCustomCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        mGattHashMap.get(address).setCharacteristicNotification(mCustomCharacteristic, true);
                        BluetoothGattDescriptor mDescriptor = mCustomCharacteristic.getDescriptor(UUID.fromString(WandAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION));
                        mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mGattHashMap.get(address).writeDescriptor(mDescriptor);
                    }
                }

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, address);
            } else {
                L.warning("onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String address = gatt.getDevice().getAddress();
            String value = new String(characteristic.getValue());

            switch (value) {
                case WandAttributes.ENTER_PASSWORD_OK:
                    if (mGattHashMap.containsKey(address)) {
                        if (!mConnectedAddresses.contains(address)) {
                            mConnectedAddresses.add(address);
                        }
                    }
                    break;
                case WandAttributes.ENTER_PASSWORD_ERROR:
                    if (mGattHashMap.containsKey(address)) {
                        mGattHashMap.get(address).disconnect();
                        mGattHashMap.get(address).close();
                        mGattHashMap.remove(address);
                    }
                    break;
            }

            L.debug("onCharacteristicChanged() - Address: " + address + " Data: " + value);
            broadcastUpdate(ACTION_DATA_AVAILABLE, address, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String address) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String address, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
        final byte[] data = characteristic.getValue();
        intent.putExtra(EXTRA_DATA, new String(data));
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

        if (mConnectedAddresses == null)
            mConnectedAddresses = new ArrayList<>();

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
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            if (mGattHashMap.get(address) != null) {
                mGattHashMap.get(address).disconnect();
            }

            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

            // We want to connect automatically to the device, so we are setting the autoConnect
            // parameter to true.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                BluetoothGatt bluetoothGatt = device.connectGatt(this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
                refreshDeviceCache(bluetoothGatt);
                mGattHashMap.put(address, bluetoothGatt);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // From Android LOLLIPOP (21) the transport types exists, but them are hide for use,
                // so is needed to use reflection to get the value
                try {
                    Method connectGattMethod = device.getClass().getDeclaredMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
                    connectGattMethod.setAccessible(true);
                    BluetoothGatt bluetoothGatt = (BluetoothGatt) connectGattMethod.invoke(device, this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
                    mGattHashMap.put(address, bluetoothGatt);
                } catch (Exception ex) {
                    L.error("Error on call BluetoothDevice.connectGatt with reflection." + ex);
                }
            }

            // If any try is fail, then call the connectGatt without transport
            if (mGattHashMap.get(address) == null) {
                mGattHashMap.put(address, device.connectGatt(this, false, mGattCallback));
            }

            L.debug("Trying to create a new connection.");
            mConnectionState = STATE_CONNECTING;
            return true;
        }
        return false;
    }

    /**
     * Call to private Android method 'refresh'
     * This method does actually clear the cache from a bluetooth device.
     * But the problem is that we don't have access to it. But in java we have reflection, so we can access this method.
     * http://stackoverflow.com/questions/22596951/how-to-programmatically-force-bluetooth-low-energy-service-discovery-on-android
     */
    public void refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Method localMethod = gatt.getClass().getMethod("refresh");
            if (localMethod != null) {
                boolean result = (Boolean) localMethod.invoke(gatt);
                if (result) {
                    L.debug("Bluetooth refresh cache");
                }
            }
        } catch (Exception localException) {
            L.error("An exception occurred while refreshing device");
        }
    }

    public void disconnect(final String address) {
        if (mConnectedAddresses.contains(address)) {
            mConnectedAddresses.remove(address);
        }
        if (mGattHashMap.containsKey(address)) {
            mGattHashMap.get(address).disconnect();
        }
    }

    public void closeConnection(String address) {
        if (mGattHashMap != null && mConnectedAddresses != null) {
            if (mConnectedAddresses.contains(address)) {
                mConnectedAddresses.remove(address);
            }
            if (mGattHashMap.containsKey(address)) {
                mGattHashMap.get(address).close();
                mGattHashMap.remove(address);
            }
        }
    }

    public void closeGattConnections() {
        if (mGattHashMap != null && mConnectedAddresses != null) {
            for (String address : mConnectedAddresses) {
                if (mConnectedAddresses.contains(address)) {
                    mConnectedAddresses.remove(address);
                }
                if (mGattHashMap.containsKey(address)) {
                    mGattHashMap.get(address).disconnect();
                    mGattHashMap.get(address).close();
                    mGattHashMap.remove(address);
                }
            }
        }
        mGattHashMap = null;
    }

    public boolean writeCharacteristic(String address, String value) {
        if (mBluetoothAdapter == null || mGattHashMap.get(address) == null) {
            L.warning("BluetoothAdapter not initialized");
            return false;
        }
        /* check if the service is available on the device */
        BluetoothGattService mService = mGattHashMap.get(address).getService(UUID.fromString(WandAttributes.WAND_SERVICE));
        if (mService == null) {
            L.warning("Wand BLE Service not found");
            return false;
        }
        /* get the writable & readable characteristic from the service */
        BluetoothGattCharacteristic mCharacteristic = mService.getCharacteristic(UUID.fromString(WandAttributes.WAND_CHARACTERISTIC));
        if (mCharacteristic == null) {
            L.warning("Wand BLE Characteristic not found");
            broadcastUpdate(ERROR_CONFIGURATION);
            return false;
        }
        /* add value to write in characteristic */
        mCharacteristic.setValue(value);
        mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        if (!mGattHashMap.get(address).writeCharacteristic(mCharacteristic)) {
            L.warning("Failed to write characteristic");
            return false;
        } else {
            return true;
        }
    }
}
