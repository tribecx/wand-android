package com.tunashields.wand.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

    private ProgressDialogFragment mProgressDialogFragment;

    private BluetoothLeService mBluetoothLeService;

    private String mPassword = null;

    private boolean autoSendPassword = false;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) iBinder).getService();
            if (!mBluetoothLeService.initialize()) {
                L.error("Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
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
                L.debug(data);
                processData(data);
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
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            L.debug("Connect request result=" + result);
        }
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

        mEnterPasswordView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                autoSendPassword = false;
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mEnterPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_GO) {
                    autoSendPassword = true;
                    sendPassword();
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
                if (autoSendPassword)
                    sendPassword();
                break;
            case WandAttributes.ENTER_PASSWORD_OK:
                if (Database.mWandDeviceDao.addDevice(new WandDevice(mDeviceAddress, mDeviceName, mPassword))) {
                    dismissProgressDialog();
                    showDoneDialog();
                }
                break;
            case WandAttributes.ENTER_PASSWORD_ERROR:
                dismissProgressDialog();
                showErrorDialog();
                break;
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void sendPassword() {
        if (mEnterPasswordView == null) return;

        mPassword = mEnterPasswordView.getText().toString();

        if (TextUtils.isEmpty(mPassword)) {
            mEnterPasswordView.setText(getString(R.string.error_empty_field));
            mEnterPasswordView.requestFocus();
            return;
        }

        showProgressDialog(getString(R.string.prompt_linking_device));

        mBluetoothLeService.writeCharacteristic(WandUtils.setEnterPasswordFormat(mPassword));
    }

    private void showProgressDialog(String message) {
        mProgressDialogFragment = ProgressDialogFragment.newInstance(message);
        mProgressDialogFragment.setCancelable(false);
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
        ErrorDialogFragment mErrorDialogFragment = ErrorDialogFragment.newInstance(true);
        mErrorDialogFragment.show(getSupportFragmentManager(), "error_dialog");
    }
}
