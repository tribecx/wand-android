package com.tunashields.wand.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;

import com.tunashields.wand.R;
import com.tunashields.wand.bluetooth.BluetoothLeService;
import com.tunashields.wand.bluetooth.WandAttributes;
import com.tunashields.wand.models.WandDevice;
import com.tunashields.wand.utils.L;
import com.tunashields.wand.utils.WandUtils;

public class DeviceDetailActivity extends AppCompatActivity {

    private WandDevice mWandDevice;

    private Button mLockDeviceButton;

    private BluetoothLeService mBluetoothLeService;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) iBinder).getService();
            if (!mBluetoothLeService.initialize()) {
                L.error("Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mWandDevice.address);
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
        setContentView(R.layout.activity_device_detail);

        if (getIntent().getExtras() == null)
            finish();

        mWandDevice = getIntent().getExtras().getParcelable(WandDevice.KEY);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        setUpViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mWandDevice.address);
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

    private void setUpViews() {
        mLockDeviceButton = (Button) findViewById(R.id.button_lock);
        mLockDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mWandDevice.mode != null && mWandDevice.mode.equals("A"))
                    return;

                if (mWandDevice.relay == 0) {
                    mBluetoothLeService.writeCharacteristic(WandUtils.setRelayFormat(1));
                } else {
                    mBluetoothLeService.writeCharacteristic(WandUtils.setRelayFormat(0));
                }
            }
        });
        updateUI();
    }

    private void updateUI() {
        if (mWandDevice.mode != null && mWandDevice.mode.equals("A")) {
            mLockDeviceButton.setText("");
            mLockDeviceButton.setBackgroundResource(R.drawable.background_automatic_lock_button);
            mLockDeviceButton.setClickable(false);
        } else {
            if (mWandDevice.relay == 0) {
                mLockDeviceButton.setBackgroundResource(R.drawable.background_green_borders_button);
                mLockDeviceButton.setText(getString(R.string.label_lock));
            } else {
                Resources resources = getResources();
                int vertical_margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, resources.getDisplayMetrics());
                int horizontal_margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, resources.getDisplayMetrics());

                LayerDrawable layerDrawable = (LayerDrawable) getDrawable(R.drawable.background_locked_device_button);
                if (layerDrawable != null && layerDrawable.getDrawable(1) != null)
                    layerDrawable.setLayerInset(1, horizontal_margin, vertical_margin, horizontal_margin, vertical_margin);

                mLockDeviceButton.setText("");
                mLockDeviceButton.setBackground(layerDrawable);
            }
        }
    }

    private void processData(String data) {
        switch (data) {
            case WandAttributes.DETECT_NEW_CONNECTION:
                mBluetoothLeService.writeCharacteristic(WandUtils.setEnterPasswordFormat(mWandDevice.password));
                break;
            case WandAttributes.ENABLE_RELAY_OK:
                mWandDevice.relay = 1;
                updateUI();
                break;
            case WandAttributes.DISABLE_RELAY_OK:
                mWandDevice.relay = 0;
                updateUI();
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
}
