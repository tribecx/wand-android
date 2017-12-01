package com.tunashields.wand.activities;

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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.tunashields.wand.R;
import com.tunashields.wand.bluetooth.WandAttributes;
import com.tunashields.wand.data.Database;
import com.tunashields.wand.fragments.AssignNameFragment;
import com.tunashields.wand.fragments.AssignOwnerFragment;
import com.tunashields.wand.fragments.AssignPasswordFragment;
import com.tunashields.wand.fragments.DoneDialogFragment;
import com.tunashields.wand.fragments.ErrorDialogFragment;
import com.tunashields.wand.fragments.ProgressDialogFragment;
import com.tunashields.wand.models.WandDevice;
import com.tunashields.wand.utils.L;
import com.tunashields.wand.utils.WandUtils;

import java.lang.reflect.Method;
import java.util.UUID;

public class CustomizeDeviceActivity extends AppCompatActivity
        implements AssignNameFragment.AssignNameListener, AssignOwnerFragment.AssignOwnerListener, AssignPasswordFragment.AssignPasswordListener {

    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    private String mDeviceName;
    private String mDeviceAddress;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic;

    private Handler mCantConnectHandler;
    private Runnable mCantConnectRunnable;

    private String mCustomName = null;
    private String mCustomOwner = null;
    private String mCustomPassword = null;

    private String mStatus = null;

    private ProgressDialogFragment mProgressDialogFragment;

    private boolean isShowingErrorMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_device);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setTitle(null);
        }

        Bundle extras = getIntent().getExtras();
        mDeviceName = extras.getString(EXTRA_DEVICE_NAME);
        mDeviceAddress = extras.getString(EXTRA_DEVICE_ADDRESS);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        showProgressDialog(getString(R.string.prompt_linking_device));

        mCantConnectRunnable = new Runnable() {
            @Override
            public void run() {
                closeConnection();
                showErrorDialog();
            }
        };
        mCantConnectHandler = new Handler();
        mCantConnectHandler.postDelayed(mCantConnectRunnable, 60 * 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDeviceAddress != null) {
            connect(mDeviceAddress);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCantConnectHandler != null && mCantConnectRunnable != null)
            mCantConnectHandler.removeCallbacks(mCantConnectRunnable);
        closeConnection();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onAssignName(String name) {
        L.debug(name);
        mCustomName = name;
        showAssignOwnerFragment();
    }

    @Override
    public void onAssignOwner(String owner) {
        L.debug(owner);
        mCustomOwner = owner;
        showAssignPasswordFragment();
    }

    @Override
    public void onAssignPassword(String password) {
        L.debug(password);
        mCustomPassword = password;
        showProgressDialog(getString(R.string.prompt_configuring));
        configurePassword();
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            L.warning("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mDeviceAddress != null && address.equals(mDeviceAddress)
                && mBluetoothGatt != null) {
            L.debug("Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                L.debug("re-connect :true");
                return true;
            } else {
                return false;
            }
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            L.debug("Device not found.  Unable to connect.");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
            refreshDeviceCache(mBluetoothGatt);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // From Android LOLLIPOP (21) the transport types exists, but them are hide for use,
            // so is needed to use reflection to get the value
            try {
                Method connectGattMethod = device.getClass().getDeclaredMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
                connectGattMethod.setAccessible(true);
                mBluetoothGatt = (BluetoothGatt) connectGattMethod.invoke(device, this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
            } catch (Exception ex) {
                L.error("Error on call BluetoothDevice.connectGatt with reflection." + ex);
            }
        }

        // If any try is fail, then call the connectGatt without transport
        if (mBluetoothGatt == null) {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }

        L.debug("Trying to create a new connection.");
        return true;
    }

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

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                L.info("Connected to device.");

                // Attempts to discover services after successful connection.
                boolean didStartServicesDiscovery = mBluetoothGatt.discoverServices();
                L.info("Attempting to start service discovery: " + didStartServicesDiscovery);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                L.info("Disconnected from device.");
                if (!isShowingErrorMessage && mDeviceAddress != null)
                    connect(mDeviceAddress);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                L.debug("Discover services status: SUCCESS");

                /**
                 * Looking for Wand BLE Service.
                 * */
                BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(WandAttributes.WAND_SERVICE));
                if (mCustomService != null) {
                    L.debug("Wand service founded: " + mCustomService.getUuid().toString());
                    /**
                     * Subscribing Wand BLE Characteristic to notifications.
                     * */
                    mCharacteristic = mCustomService.getCharacteristic(UUID.fromString(WandAttributes.WAND_CHARACTERISTIC));
                    if (mCharacteristic != null) {
                        L.debug("Wand characteristic founded: " + mCharacteristic.getUuid().toString());
                        if ((mCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            mBluetoothGatt.setCharacteristicNotification(mCharacteristic, false);
                            //mGattHashMap.get(address).readCharacteristic(mCharacteristic);
                        }

                        mBluetoothGatt.setCharacteristicNotification(mCharacteristic, true);
                        BluetoothGattDescriptor mDescriptor = mCharacteristic.getDescriptor(UUID.fromString(WandAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION));
                        mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        mBluetoothGatt.writeDescriptor(mDescriptor);
                    }
                }
            } else {
                L.warning("onServicesDiscovered received: error - status: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            String address = gatt.getDevice().getAddress();
            String value = new String(characteristic.getValue());
            L.debug("onCharacteristicRead() - Address: " + address + " Data: " + value);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String address = gatt.getDevice().getAddress();
            String data = new String(characteristic.getValue());

            L.debug("onCharacteristicChanged() - Address: " + address + " Data: " + data);

            processData(data);
        }
    };

    private void closeConnection() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private void processData(String data) {
        switch (data) {
            case WandAttributes.DETECT_NEW_CONNECTION:
                if (mStatus == null) {
                    mCantConnectHandler.removeCallbacks(mCantConnectRunnable);
                    mStatus = WandAttributes.DETECT_NEW_CONNECTION;
                    writeCharacteristic(WandUtils.setEnterPasswordFormat(WandAttributes.DEFAULT_PASSWORD));
                }
                if (mStatus != null && mStatus.equals(WandAttributes.CHANGE_PASSWORD_OK)) {
                    writeCharacteristic(WandUtils.setEnterPasswordFormat(mCustomPassword));
                }
                break;
            case WandAttributes.ENTER_PASSWORD_OK:
                if (mStatus.equals(WandAttributes.DETECT_NEW_CONNECTION)) {
                    mStatus = WandAttributes.ENTER_PASSWORD_OK;
                    L.debug("Enter password OK");
                    dismissProgressDialog();
                    showAssignNameFragment();
                }
                break;
            case WandAttributes.CHANGE_PASSWORD_OK:
                if (mStatus.equals(WandAttributes.ENTER_PASSWORD_OK)) {
                    mStatus = WandAttributes.CHANGE_PASSWORD_OK;
                    L.debug("Password changed correctly");

                    configureNameAndOwner();
                }
                break;
            case WandAttributes.CHANGE_NAME_OK:
                if (mStatus.equals(WandAttributes.CHANGE_PASSWORD_OK)) {
                    if (Database.mWandDeviceDao.getDeviceByAddress(mDeviceAddress) == null) {
                        if (Database.mWandDeviceDao.addDevice(new WandDevice(mDeviceAddress, mCustomName, mCustomOwner, mCustomPassword, "M", 0, true))) {
                            L.debug("Device " + mCustomName + " of " + mCustomOwner + " added");
                            closeConnection();
                            dismissProgressDialog();
                            showDoneDialog();
                        }
                    }
                }
                break;
            case WandAttributes.ENTER_PASSWORD_ERROR:
            case WandAttributes.CHANGE_PASSWORD_ERROR:
                closeConnection();
                if (mCantConnectHandler != null && mCantConnectRunnable != null)
                    mCantConnectHandler.removeCallbacks(mCantConnectRunnable);
                showErrorDialog();
                break;
        }
    }

    public void writeCharacteristic(String value) {
        if (mBluetoothAdapter == null) {
            L.warning("BluetoothAdapter not initialized");
            return;
        }

        if (mCharacteristic == null) {
            L.warning("Wand BLE Characteristic not found");
            return;
        }

        /* add value to write in characteristic */
        mCharacteristic.setValue(value);
        mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        if (!mBluetoothGatt.writeCharacteristic(mCharacteristic)) {
            L.warning("Failed to write characteristic");
        } else {
            L.debug("Correctly written value: " + value);
        }
    }

    private void showAssignNameFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.layout_customize_device_content, new AssignNameFragment())
                .commit();
    }

    private void showAssignOwnerFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.layout_customize_device_content, new AssignOwnerFragment())
                .addToBackStack(null)
                .commit();
    }

    private void showAssignPasswordFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.layout_customize_device_content, new AssignPasswordFragment())
                .addToBackStack(null)
                .commit();
    }

    private void configureNameAndOwner() {
        L.debug("Configuring name and owner");
        writeCharacteristic(WandUtils.setChangeNameAndOwnerFormat(mCustomName, mCustomOwner));
    }

    private void configurePassword() {
        L.debug("Configuring password");
        writeCharacteristic(WandUtils.setChangePasswordFormat(mCustomPassword));
    }

    private void showProgressDialog(String message) {
        mProgressDialogFragment = ProgressDialogFragment.newInstance(message);
        mProgressDialogFragment.setCancelable(false);
        mProgressDialogFragment.setOnCancelClickListener(new ProgressDialogFragment.OnCancelClickListener() {
            @Override
            public void onCancel() {
                closeConnection();
                finish();
            }
        });
        mProgressDialogFragment.show(getSupportFragmentManager(), "progress_dialog");
    }

    private void dismissProgressDialog() {
        if (mProgressDialogFragment != null) {
            mProgressDialogFragment.dismiss();
        }
    }

    private void showDoneDialog() {
        DoneDialogFragment mDoneDialogFragment = DoneDialogFragment.newInstance(getString(R.string.label_name_and_owner_added_properly));
        mDoneDialogFragment.setCancelable(false);
        mDoneDialogFragment.show(getSupportFragmentManager(), "done_dialog");
    }

    private void showErrorDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ErrorDialogFragment mErrorDialogFragment = ErrorDialogFragment.newInstance(true);
                mErrorDialogFragment.show(getSupportFragmentManager(), "error_dialog");
                isShowingErrorMessage = true;
            }
        });
    }
}
