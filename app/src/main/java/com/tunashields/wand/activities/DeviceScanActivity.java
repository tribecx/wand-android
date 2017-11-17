package com.tunashields.wand.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tunashields.wand.R;
import com.tunashields.wand.adapters.WandDevicesAdapter;
import com.tunashields.wand.bluetooth.WandAttributes;
import com.tunashields.wand.data.Database;
import com.tunashields.wand.models.WandDevice;
import com.tunashields.wand.utils.L;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeviceScanActivity extends AppCompatActivity {

    ImageView mProgressView;
    TextView mLookingDevicesView;

    private ArrayList<WandDevice> mPairedDevices;
    private ArrayList<String> mPairedDevicesAddresses;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings mScanSettings;
    private List<ScanFilter> mScanFilters;

    private int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;

    private WandDevicesAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        mProgressView = (ImageView) findViewById(R.id.image_looking_devices_progress);
        mLookingDevicesView = (TextView) findViewById(R.id.text_looking_devices);

        mPairedDevices = Database.mWandDeviceDao.getAllDevices();
        mPairedDevicesAddresses = new ArrayList<>();
        for (int i = 0; i < mPairedDevices.size(); i++) {
            mPairedDevicesAddresses.add(mPairedDevices.get(i).address);
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mHandler = new Handler();

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recycler_add_device);
        mAdapter = new WandDevicesAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(new WandDevicesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, Object object) {
                mLeScanner.stopScan(mScanCallback);
                BluetoothDevice bluetoothDevice = (BluetoothDevice) object;
                Intent intent = new Intent(DeviceScanActivity.this, PairDeviceActivity.class);
                intent.putExtra(PairDeviceActivity.EXTRA_DEVICE_NAME, bluetoothDevice.getName());
                intent.putExtra(PairDeviceActivity.EXTRA_DEVICE_ADDRESS, bluetoothDevice.getAddress());
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mScanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            mScanFilters = new ArrayList<>();

            ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(
                    new ParcelUuid(UUID.fromString(WandAttributes.WAND_ADVERTISEMENT_DATA_UUID))).build();

            mScanFilters.add(scanFilter);

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
        /*if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }*/
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
            mLeScanner.startScan(mScanFilters, mScanSettings, mScanCallback);
            showProgress();
        } else {
            mLeScanner.stopScan(mScanCallback);
            hideProgress();
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    L.debug("callbackType: " + String.valueOf(callbackType));
                    L.debug("ScanResult: " + result.toString());
                    BluetoothDevice btDevice = result.getDevice();

                    if (result.toString().contains(WandAttributes.WAND_ADVERTISEMENT_DATA_UUID)
                            && !mAdapter.contains(btDevice)
                            && !mPairedDevicesAddresses.contains(btDevice.getAddress())) {
                        mAdapter.add(btDevice);
                    }
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
            L.error("Scan Failed  - Error Code: " + errorCode);
        }
    };

    /*public void connectToDevice(BluetoothDevice device) {
        if (mBluetoothGatt == null) {
            mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }*/

    /*private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            L.error("onConnectionStateChange Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    L.info("mGattCallback: STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    L.error("mGattCallback: STATE_DISCONNECTED");
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            L.info("onServicesDiscovered " + services.toString());

            BluetoothGattCharacteristic characteristic = services.get(4).getCharacteristics().get(0);
            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(WandAttributes.CLIENT_CHARACTERISTIC_CONFIGURATION));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            L.info("onCharacteristicRead " + characteristic.toString());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] value = characteristic.getValue();
            final String v = new String(value);
            L.info("onCharacteristicChanged: Value = " + v);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((TextView) findViewById(R.id.text_status)).setText(v);
                }
            });
        }
    };*/

    private void showProgress() {
        mProgressView.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(DeviceScanActivity.this, R.anim.rotate);
        mProgressView.setAnimation(animation);
        mLookingDevicesView.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mProgressView.setAnimation(null);
        mProgressView.setVisibility(View.GONE);
        mLookingDevicesView.setVisibility(View.GONE);
    }

    public void cancel(View view) {
        finish();
    }
}
