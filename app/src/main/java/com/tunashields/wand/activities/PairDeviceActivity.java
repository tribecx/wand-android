package com.tunashields.wand.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.tunashields.wand.R;
import com.tunashields.wand.bluetooth.BluetoothLeService;
import com.tunashields.wand.bluetooth.WandAttributes;
import com.tunashields.wand.data.Database;
import com.tunashields.wand.fragments.DoneDialogFragment;
import com.tunashields.wand.fragments.ErrorDialogFragment;
import com.tunashields.wand.fragments.ProgressDialogFragment;
import com.tunashields.wand.models.WandDevice;
import com.tunashields.wand.utils.L;
import com.tunashields.wand.utils.WandUtils;

public class PairDeviceActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    private String mDeviceName;
    private String mDeviceAddress;

    private EditText mEnterPasswordView;

    private BluetoothLeService mBluetoothLeService;

    private Handler mCantConnectHandler;
    private Runnable mCantConnectRunnable;

    private String mPassword = null;

    private WandDevice mWandDevice;

    private ProgressDialogFragment mProgressDialogFragment;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) iBinder).getService();
            if (!mBluetoothLeService.initialize()) {
                L.error("Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                processData(data);
            }
            if (BluetoothLeService.ERROR_CONFIGURATION.equals(action)) {
                dismissProgressDialog();
                if (mCantConnectHandler != null && mCantConnectRunnable != null)
                    mCantConnectHandler.removeCallbacks(mCantConnectRunnable);
                showErrorDialog();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair_device);

        Bundle extras = getIntent().getExtras();
        mDeviceName = extras.getString(EXTRA_DEVICE_NAME);
        mDeviceAddress = extras.getString(EXTRA_DEVICE_ADDRESS);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        if (mDeviceName.contains(WandAttributes.NEW_DEVICE_KEY)) {
            showAddDeviceDialog();
        } else {
            showPairDeviceScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
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
                                if (mBluetoothLeService != null) {
                                    mBluetoothLeService.disconnect(mDeviceAddress);
                                    mBluetoothLeService.closeConnection(mDeviceAddress);
                                }
                                showErrorDialog();
                            }
                        };
                        mCantConnectHandler = new Handler();
                        mCantConnectHandler.postDelayed(mCantConnectRunnable, 60 * 1000);

                        mBluetoothLeService.connect(mDeviceAddress);
                    }
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
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
                            dismissProgressDialog();
                            showDoneDialog();
                        }
                    }
                }

                if (data.contains("#P:NO@")) {
                    dismissProgressDialog();
                    showErrorDialog();
                }
                break;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ERROR_CONFIGURATION);
        return intentFilter;
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
        mBluetoothLeService.writeCharacteristic(mDeviceAddress, WandUtils.setEnterPasswordFormat(mPassword));
    }

    private void getOwner() {
        mBluetoothLeService.writeCharacteristic(mDeviceAddress, WandUtils.getOwner());
    }

    private void getState() {
        mBluetoothLeService.writeCharacteristic(mDeviceAddress, WandUtils.getState());
    }

    private void showProgressDialog(String message) {
        mProgressDialogFragment = ProgressDialogFragment.newInstance(message);
        mProgressDialogFragment.setCancelable(false);
        mProgressDialogFragment.setOnCancelClickListener(new ProgressDialogFragment.OnCancelClickListener() {
            @Override
            public void onCancel() {
                if (mBluetoothLeService != null) {
                    mBluetoothLeService.disconnect(mDeviceAddress);
                    mBluetoothLeService.closeConnection(mDeviceAddress);
                }
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
            }
        });
    }
}
