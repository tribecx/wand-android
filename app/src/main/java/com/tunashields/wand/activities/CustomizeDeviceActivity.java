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
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.tunashields.wand.R;
import com.tunashields.wand.bluetooth.BluetoothLeService;
import com.tunashields.wand.bluetooth.WandAttributes;
import com.tunashields.wand.fragments.AssignNameFragment;
import com.tunashields.wand.fragments.AssignOwnerFragment;
import com.tunashields.wand.fragments.AssignPasswordFragment;
import com.tunashields.wand.utils.L;

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
                customize(data);
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

        showLinkingDialog(true);
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

    private void customize(String data) {
        switch (data) {
            case WandAttributes.DETECT_NEW_CONNECTION:
                mBluetoothLeService.writeCharacteristic(WandAttributes.DEFAULT_PASSWORD);
                break;
            case WandAttributes.DEFAULT_PASSWORD_OK:
                showLinkingDialog(false);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.layout_customize_device_content, new AssignNameFragment())
                        .commit();
                break;
            case WandAttributes.NAME_OK:
                configurePassword();
                break;
            case WandAttributes.PASSWORD_OK:
                Toast.makeText(CustomizeDeviceActivity.this, "Dispositivo configurado correctamente", Toast.LENGTH_LONG).show();
                showConfiguringDialog(false);
                finish();
                break;
        }
    }

    @Override
    public void onAssignName(String name) {
        L.debug(name);
        mCustomName = name;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.layout_customize_device_content, new AssignOwnerFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onAssignOwner(String owner) {
        L.debug(owner);
        mCustomOwner = owner;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.layout_customize_device_content, new AssignPasswordFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onAssignPassword(String password) {
        L.debug(password);
        mCustomPassword = password;
        showConfiguringDialog(true);
        configureNameAndOwner();
    }

    private void configureNameAndOwner() {
        String name_owner = "#N" + mCustomName + "-" + mCustomOwner + "@";
        mBluetoothLeService.writeCharacteristic(name_owner);
    }

    private void configurePassword() {
        mBluetoothLeService.writeCharacteristic(mCustomPassword);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void showLinkingDialog(boolean value) {
        Animation animation = AnimationUtils.loadAnimation(CustomizeDeviceActivity.this, R.anim.rotate);
        findViewById(R.id.image_progress).setAnimation(value ? animation : null);
        ((TextView) findViewById(R.id.text_progress_message)).setText(getString(R.string.prompt_linking_device));
        findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        findViewById(R.id.linking_device_dialog).setVisibility(value ? View.VISIBLE : View.GONE);
        findViewById(R.id.layout_customize_device_content).setVisibility(value ? View.GONE : View.VISIBLE);
    }

    private void showConfiguringDialog(boolean value) {
        Animation animation = AnimationUtils.loadAnimation(CustomizeDeviceActivity.this, R.anim.rotate);
        findViewById(R.id.image_progress).setAnimation(value ? animation : null);
        ((TextView) findViewById(R.id.text_progress_message)).setText(getString(R.string.prompt_configuring));
        findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        findViewById(R.id.linking_device_dialog).setVisibility(value ? View.VISIBLE : View.GONE);
        findViewById(R.id.layout_customize_device_content).setVisibility(value ? View.GONE : View.VISIBLE);
    }
}
