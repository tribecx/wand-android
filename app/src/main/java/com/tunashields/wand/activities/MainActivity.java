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
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.tunashields.wand.R;
import com.tunashields.wand.adapters.WandDevicesAdapter;
import com.tunashields.wand.data.Database;
import com.tunashields.wand.models.WandDevice;
import com.tunashields.wand.utils.L;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ArrayList<WandDevice> mPairedDevices;
    private ArrayList<String> mPairedDevicesAddresses;
    private ArrayList<String> mFoundedDevicesAddresses;
    private RecyclerView mRecyclerView;
    private WandDevicesAdapter mAdapter;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings mScanSettings;
    private List<ScanFilter> mScanFilters;
    private static final long SCAN_PERIOD = 10000;

    private final int MY_REQUEST_ENABLE_BT = 101;
    private final int MY_REQUEST_ENABLE_GPS = 102;
    private final int MY_CONFIGURE_DEVICE_REQUEST = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPairedDevices = Database.mWandDeviceDao.getAllDevices();
        mPairedDevicesAddresses = new ArrayList<>();
        for (int i = 0; i < mPairedDevices.size(); i++) {
            mPairedDevicesAddresses.add(mPairedDevices.get(i).address);
        }
        mFoundedDevicesAddresses = new ArrayList<>();

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_paired_devices);
        mAdapter = new WandDevicesAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.addAll(mPairedDevices);
        mAdapter.setOnItemClickListener(new WandDevicesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(WandDevice wandDevice) {
                Intent intent = new Intent(MainActivity.this, DeviceDetailActivity.class);
                intent.putExtra(WandDevice.KEY, wandDevice);
                startActivityForResult(intent, MY_CONFIGURE_DEVICE_REQUEST);
            }

            @Override
            public void onItemClick(BluetoothDevice bluetoothDevice) {

            }
        });

        mAdapter.setOnLockClickListener(new WandDevicesAdapter.OnLockClickListener() {
            @Override
            public void onLock() {
                /*if (isLocked) {
                    //unlock(address);
                } else {
                    //lock(address);
                }*/
            }
        });

        setVisibleLayout(mAdapter.getItemCount() <= 0);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mHandler = new Handler();

        if (checkGPS()) {
            checkBluetooth();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mScanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            mScanFilters = new ArrayList<>();
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MY_REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_CANCELED) {
                    //Bluetooth not enabled.
                    finish();
                }
                break;
            case MY_REQUEST_ENABLE_GPS:
                LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    checkBluetooth();
                } else {
                    finish();
                }
                break;
            case MY_CONFIGURE_DEVICE_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    mFoundedDevicesAddresses.clear();
                    mAdapter.clear();
                    mAdapter.addAll(Database.mWandDeviceDao.getAllDevices());
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void setVisibleLayout(boolean value) {
        findViewById(R.id.image_no_paired_devices).setVisibility(value ? View.VISIBLE : View.GONE);
        findViewById(R.id.layout_paired_devices).setVisibility(value ? View.GONE : View.VISIBLE);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLeScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);
            mLeScanner.startScan(mScanFilters, mScanSettings, mScanCallback);
        } else {
            mLeScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            L.info("callbackType: " + String.valueOf(callbackType));
            L.info("ScanResult: " + result.toString());
            BluetoothDevice btDevice = result.getDevice();

            String address = btDevice.getAddress();

            if (mPairedDevicesAddresses.contains(address) && !mFoundedDevicesAddresses.contains(address)) {
                mFoundedDevicesAddresses.add(address);
                mAdapter.notifyDeviceFounded(address);
            }

            /*WandDevice device = Database.mWandDeviceDao.getDeviceByAddress(btDevice.getAddress());
            if (device != null && !mAdapter.contains(device)) {
                mAdapter.add(device);
                setVisibleLayout(mAdapter.getItemCount() <= 0);
            }*/

            /*if (result.toString().contains(WandAttributes.WAND_ADVERTISEMENT_DATA_UUID)
                    && !mAdapter.contains(btDevice)
                    && !mPairedDevicesAddresses.contains(btDevice.getAddress())) {
                mAdapter.add(btDevice);
            }*/
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                L.info("ScanResult - Results: " + sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            L.error("Scan Failed  - Error Code: " + errorCode);
        }
    };

    public void checkBluetooth() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            mDialogBuilder.setTitle(getString(R.string.warning_disable_bluetooth_title));
            mDialogBuilder.setMessage(getString(R.string.warning_disable_bluetooth_message));
            mDialogBuilder.setNegativeButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    finish();
                }
            });
            mDialogBuilder.setPositiveButton(getString(R.string.label_activate), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, MY_REQUEST_ENABLE_BT);
                }
            });
            mDialogBuilder.show();
        }
    }

    public void startAddDeviceActivity(View view) {
        startActivity(new Intent(MainActivity.this, DeviceScanActivity.class));
    }

    public void startSettingsActivity(View view) {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
    }

    public boolean checkGPS() {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.warning_disable_gps_message));
            builder.setPositiveButton(getString(R.string.label_enable), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(callGPSSettingIntent, MY_REQUEST_ENABLE_GPS);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    finish();
                }
            });
            builder.setCancelable(false);
            builder.create().show();
            return false;
        } else {
            return true;
        }
    }
}
