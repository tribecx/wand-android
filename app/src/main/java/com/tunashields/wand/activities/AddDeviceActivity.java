package com.tunashields.wand.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tunashields.wand.BuildConfig;
import com.tunashields.wand.R;
import com.tunashields.wand.adapters.WandDevicesAdapter;
import com.tunashields.wand.utils.WandUtils;

import java.util.ArrayList;
import java.util.List;

public class AddDeviceActivity extends AppCompatActivity {

    private final String TAG = AddDeviceActivity.class.getSimpleName();

    ImageView mProgressView;
    TextView mLookingDevicesView;

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;

    private WandDevicesAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        mProgressView = (ImageView) findViewById(R.id.image_looking_devices);
        mLookingDevicesView = (TextView) findViewById(R.id.text_looking_devices);

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recycler_add_device);
        mAdapter = new WandDevicesAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGatt != null) {
            mGatt.close();
            mGatt = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLeScanner.stopScan(mScanCallback);
                    hideProgress();
                }
            }, SCAN_PERIOD);
            mLeScanner.startScan(filters, settings, mScanCallback);
            showProgress();
        } else {
            mLeScanner.stopScan(mScanCallback);
            hideProgress();
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            logInfo("callbackType: " + String.valueOf(callbackType));
            logInfo("ScanResult: " + result.toString());
            BluetoothDevice btDevice = result.getDevice();
            /*connectToDevice(btDevice);*/

            if (result.toString().contains(WandUtils.WAND_SERVICE) && !mAdapter.contains(btDevice)) {
                mAdapter.add(btDevice);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                logInfo("ScanResult - Results: " + sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            logError("Scan Failed  - Error Code: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            logInfo("onConnectionStateChange Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    logInfo("gattCallback: STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    logError("gattCallback: STATE_DISCONNECTED");
                    break;
                default:
                    logError("gattCallback: STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            logInfo("onServicesDiscovered " + services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            logInfo("onCharacteristicRead " + characteristic.toString());
            gatt.disconnect();
        }
    };

    private void showProgress() {
        mProgressView.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(AddDeviceActivity.this, R.anim.rotate);
        mProgressView.setAnimation(animation);
        mLookingDevicesView.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mProgressView.setAnimation(null);
        mProgressView.setVisibility(View.GONE);
        mLookingDevicesView.setVisibility(View.GONE);
    }

    private void logDebug(String message) {
        if (BuildConfig.DEBUG) Log.d(TAG, message);
    }

    private void logInfo(String message) {
        if (BuildConfig.DEBUG) Log.i(TAG, message);
    }

    private void logError(String message) {
        if (BuildConfig.DEBUG) Log.e(TAG, message);
    }
}
