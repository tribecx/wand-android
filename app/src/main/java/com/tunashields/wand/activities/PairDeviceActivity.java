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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.tunashields.wand.R;
import com.tunashields.wand.bluetooth.WandAttributes;
import com.tunashields.wand.data.Database;
import com.tunashields.wand.fragments.DoneDialogFragment;
import com.tunashields.wand.fragments.ErrorDialogFragment;
import com.tunashields.wand.fragments.ProgressDialogFragment;
import com.tunashields.wand.models.WandDevice;
import com.tunashields.wand.utils.L;
import com.tunashields.wand.utils.WandUtils;

import java.lang.reflect.Method;
import java.util.UUID;

public class PairDeviceActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    private String mDeviceName;
    private String mDeviceAddress;

    private EditText mEnterPasswordView;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic;

    private Handler mCantConnectHandler;
    private Runnable mCantConnectRunnable;

    private String mPassword = null;

    private WandDevice mWandDevice;

    private ProgressDialogFragment mProgressDialogFragment;

    private boolean isShowingErrorMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_device);

        Bundle extras = getIntent().getExtras();
        mDeviceName = extras.getString(EXTRA_DEVICE_NAME);
        mDeviceAddress = extras.getString(EXTRA_DEVICE_ADDRESS);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mDeviceName.contains(WandAttributes.NEW_DEVICE_KEY)) {
            showAddDeviceDialog();
        } else {
            showPairDeviceScreen();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCantConnectHandler != null && mCantConnectRunnable != null)
            mCantConnectHandler.removeCallbacks(mCantConnectRunnable);
        closeConnection();
    }

    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.label_advertisement));
        builder.setMessage(getString(R.string.prompt_detected_new_device_will_start_configuration));
        builder.setNegativeButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(PairDeviceActivity.this, CustomizeDeviceActivity.class);
                intent.putExtra(CustomizeDeviceActivity.EXTRA_DEVICE_NAME, mDeviceName);
                intent.putExtra(CustomizeDeviceActivity.EXTRA_DEVICE_ADDRESS, mDeviceAddress);
                startActivity(intent);
                finish();
            }
        });
        builder.setCancelable(false);
        builder.create().show();
    }

    private void showPairDeviceScreen() {
        mEnterPasswordView = (EditText) findViewById(R.id.edit_enter_password);
        mEnterPasswordView.requestFocus();

        mEnterPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_GO) {
                    if (isValidPassword()) {
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

                        connect(mDeviceAddress);
                    }
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeConnection();
                finish();
            }
        });
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
                if (mCantConnectHandler != null && mCantConnectRunnable != null)
                    mCantConnectHandler.removeCallbacks(mCantConnectRunnable);
                sendPassword();
                break;
            case WandAttributes.ENTER_PASSWORD_OK:
                mWandDevice = new WandDevice(mDeviceAddress, mDeviceName, mPassword, false);
                getOwner();
                break;
            case WandAttributes.ENTER_PASSWORD_ERROR:
                dismissProgressDialog();
                if (mCantConnectHandler != null && mCantConnectRunnable != null)
                    mCantConnectHandler.removeCallbacks(mCantConnectRunnable);
                closeConnection();
                showErrorDialog();
                break;
            default:
                if (data.contains("#D:")) {
                    mWandDevice.owner = data.substring(3, data.length() - 1);
                    L.debug("Updated owner " + mWandDevice.owner);
                    getState();
                }

                if (data.contains("#E:") && data.contains("OK")) {
                    if (data.contains(WandAttributes.MODE_MANUAL)) {
                        mWandDevice.mode = "M";
                        L.debug("Updated mode status: " + WandAttributes.MODE_MANUAL);
                    } else if (data.contains(WandAttributes.MODE_AUTOMATIC)) {
                        mWandDevice.mode = "A";
                        L.debug("Updated mode status: " + WandAttributes.MODE_AUTOMATIC);
                    }

                    if (data.contains(WandAttributes.RELAY_DISABLED)) {
                        mWandDevice.relay = 0;
                        L.debug("Updated relay status: " + WandAttributes.RELAY_DISABLED);
                    } else if (data.contains(WandAttributes.RELAY_ENABLED)) {
                        mWandDevice.relay = 1;
                        L.debug("Updated relay status: " + WandAttributes.RELAY_ENABLED);
                    }

                    if (Database.mWandDeviceDao.getDeviceByAddress(mDeviceAddress) == null) {
                        if (Database.mWandDeviceDao.addDevice(mWandDevice)) {
                            L.debug("Device " + mWandDevice.name + " of " + mWandDevice.owner + " added");
                            closeConnection();
                            dismissProgressDialog();
                            showDoneDialog();
                        }
                    }
                }

                if (data.contains("#P:NO@")) {
                    dismissProgressDialog();
                    closeConnection();
                    showErrorDialog();
                }
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

    private boolean isValidPassword() {
        mEnterPasswordView.setError(null);

        mPassword = mEnterPasswordView.getText().toString();

        if (TextUtils.isEmpty(mPassword)) {
            mEnterPasswordView.setError(getString(R.string.error_empty_field));
            mEnterPasswordView.requestFocus();
            return false;
        }

        if (mPassword.length() != 5) {
            mEnterPasswordView.setError(getString(R.string.error_password_length));
            mEnterPasswordView.requestFocus();
            return false;
        }

        return true;
    }

    private void sendPassword() {
        L.debug("Sending password " + WandUtils.setEnterPasswordFormat(mPassword));
        writeCharacteristic(WandUtils.setEnterPasswordFormat(mPassword));
    }

    private void getOwner() {
        writeCharacteristic(WandUtils.getOwner());
    }

    private void getState() {
        writeCharacteristic(WandUtils.getState());
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
        DoneDialogFragment mDoneDialogFragment = DoneDialogFragment.newInstance(getString(R.string.label_device_added_correctly));
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
