package com.tunashields.wand.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.tunashields.wand.R;
import com.tunashields.wand.bluetooth.BluetoothLeService;
import com.tunashields.wand.bluetooth.WandAttributes;
import com.tunashields.wand.data.Database;
import com.tunashields.wand.fragments.AssignNameFragment;
import com.tunashields.wand.fragments.AssignOwnerFragment;
import com.tunashields.wand.fragments.AssignPasswordFragment;
import com.tunashields.wand.fragments.DoneDialogFragment;
import com.tunashields.wand.fragments.ProgressDialogFragment;
import com.tunashields.wand.models.WandDevice;
import com.tunashields.wand.utils.L;
import com.tunashields.wand.utils.WandUtils;

public class CustomizeDeviceActivity extends AppCompatActivity
        implements AssignNameFragment.AssignNameListener, AssignOwnerFragment.AssignOwnerListener, AssignPasswordFragment.AssignPasswordListener {

    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    private String mDeviceName;
    private String mDeviceAddress;

    private BluetoothLeService mBluetoothLeService;

    private String mCustomName = null;
    private String mCustomOwner = null;
    private String mCustomPassword = null;

    private String mStatus = null;

    private ProgressDialogFragment mProgressDialogFragment;

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

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        showProgressDialog(getString(R.string.prompt_linking_device));
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void processData(String data) {
        switch (data) {
            case WandAttributes.DETECT_NEW_CONNECTION:
                if (mStatus == null) {
                    mStatus = WandAttributes.DETECT_NEW_CONNECTION;
                    mBluetoothLeService.writeCharacteristic(WandUtils.setEnterPasswordFormat(WandAttributes.DEFAULT_PASSWORD));
                } else if (mStatus.equals(WandAttributes.CHANGE_PASSWORD_OK)) {
                    mBluetoothLeService.writeCharacteristic(WandUtils.setEnterPasswordFormat(mCustomPassword));
                }
                break;
            case WandAttributes.ENTER_PASSWORD_OK:
                if (mStatus.equals(WandAttributes.DETECT_NEW_CONNECTION)) {
                    mStatus = WandAttributes.ENTER_PASSWORD_OK;
                    dismissProgressDialog();
                    showAssignNameFragment();
                } else if (mStatus.equals(WandAttributes.CHANGE_PASSWORD_OK)) {
                    configureNameAndOwner();
                }
                break;
            case WandAttributes.CHANGE_PASSWORD_OK:
                if (mStatus.equals(WandAttributes.ENTER_PASSWORD_OK)) {
                    mStatus = WandAttributes.CHANGE_PASSWORD_OK;
                    configureNameAndOwner();
                }
                break;
            case WandAttributes.CHANGE_NAME_OK:
                if (mStatus.equals(WandAttributes.CHANGE_PASSWORD_OK)) {
                    if (Database.mWandDeviceDao.addDevice(new WandDevice(mDeviceAddress, mCustomName, mCustomOwner, mCustomPassword, "M", 0))) {
                        dismissProgressDialog();
                        showDoneDialog();
                    }
                }
                break;
        }
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
        mBluetoothLeService.writeCharacteristic(WandUtils.setChangeNameAndOwnerFormat(mCustomName, mCustomOwner));
    }

    private void configurePassword() {
        mBluetoothLeService.writeCharacteristic(WandUtils.setChangePasswordFormat(mCustomPassword));
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
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
        DoneDialogFragment mDoneDialogFragment = DoneDialogFragment.newInstance(getString(R.string.label_name_and_owner_added_properly));
        mDoneDialogFragment.setCancelable(false);
        mDoneDialogFragment.show(getSupportFragmentManager(), "done_dialog");
    }
}
