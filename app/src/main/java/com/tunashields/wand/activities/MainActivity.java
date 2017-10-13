package com.tunashields.wand.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.tunashields.wand.R;
import com.tunashields.wand.adapters.WandDevicesAdapter;
import com.tunashields.wand.bluetooth.BluetoothLeService;
import com.tunashields.wand.bluetooth.WandAttributes;
import com.tunashields.wand.data.Database;
import com.tunashields.wand.models.WandDevice;
import com.tunashields.wand.utils.L;
import com.tunashields.wand.utils.SwipeHelper;
import com.tunashields.wand.utils.WandUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements WandDevicesAdapter.OnItemClickListener,
        WandDevicesAdapter.OnLockClickListener, SwipeRefreshLayout.OnRefreshListener {

    private HashMap<String, WandDevice> mPairedDevicesMap;
    private ArrayList<String> mFoundedDevicesAddresses;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings mScanSettings;
    private static final long SCAN_PERIOD = 10000;

    private WandDevicesAdapter mAdapter;
    private SwipeRefreshLayout mRefreshDevices;

    private final int MY_REQUEST_ENABLE_BT = 101;
    private final int MY_REQUEST_ENABLE_GPS = 102;
    private final int MY_CONFIGURE_DEVICE_REQUEST = 103;
    private int mItemCurrentPosition;

    private BluetoothLeService mBluetoothLeService;

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
                String address = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_ADDRESS);
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);

                L.debug("Processing: " + "Address: " + address + " - Data: " + data);
                processData(address, data);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                String address = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE_ADDRESS);
                mFoundedDevicesAddresses.remove(address);
                mAdapter.notifyDeviceDisconnected(address);
                mBluetoothLeService.closeConnection(address);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindBluetoothService();
        initBluetoothElements();
        setUpListElements();
        if (checkGPS()) {
            checkBluetooth();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        initVariables();
        populateList();
        setVisibleLayout();

        if (mBluetoothLeService != null) {
            for (String address : mBluetoothLeService.mConnectedAddresses) {
                mAdapter.notifyDeviceFounded(address);
            }
        }

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mScanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService.closeGattConnections();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
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
                if (data != null && data.getExtras() != null) {
                    WandDevice device = data.getExtras().getParcelable(WandDevice.KEY);
                    if (device != null)
                        mAdapter.update(mItemCurrentPosition, device);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void bindBluetoothService() {
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    public void initBluetoothElements() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();
    }

    public void setUpListElements() {
        mAdapter = new WandDevicesAdapter(this);
        mAdapter.setOnItemClickListener(this);
        mAdapter.setOnLockClickListener(this);

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_paired_devices);
        mRecyclerView.setAdapter(mAdapter);

        new SwipeHelper(this, mRecyclerView) {
            @Override
            public void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons) {
                underlayButtons.add(new SwipeHelper.UnderlayButton(
                        getString(R.string.label_delete), 0, ContextCompat.getColor(getApplicationContext(), R.color.red),
                        new SwipeHelper.UnderlayButtonClickListener() {
                            @Override
                            public void onClick(int pos) {
                                showDeleteDeviceDialog(pos);
                            }
                        }
                ));
            }
        };

        mRefreshDevices = (SwipeRefreshLayout) findViewById(R.id.refresh_devices);
        mRefreshDevices.setOnRefreshListener(this);
    }

    public void initVariables() {
        mPairedDevicesMap = new HashMap<>();
        mFoundedDevicesAddresses = new ArrayList<>();
        ArrayList<WandDevice> mPairedDevices = Database.mWandDeviceDao.getAllDevices();
        for (int i = 0; i < mPairedDevices.size(); i++) {
            mPairedDevicesMap.put(mPairedDevices.get(i).address, mPairedDevices.get(i));
        }
    }

    private void populateList() {
        mAdapter.clear();
        mAdapter.addAll(Database.mWandDeviceDao.getAllDevices());
    }

    @Override
    public void onItemClick(int position, Object object) {
        mItemCurrentPosition = position;
        Intent intent = new Intent(MainActivity.this, DeviceDetailActivity.class);
        intent.putExtra(WandDevice.KEY, (WandDevice) object);
        startActivityForResult(intent, MY_CONFIGURE_DEVICE_REQUEST);
    }

    @Override
    public void onLock(int position, WandDevice wandDevice) {
        if (wandDevice.relay == 1) {
            // unlock
            if (mBluetoothLeService.writeCharacteristic(wandDevice.address, WandUtils.setRelayFormat(0))) {
                wandDevice.relay = 0;
                Database.mWandDeviceDao.updateDevice(wandDevice);
            }
        } else {
            // lock
            if (mBluetoothLeService.writeCharacteristic(wandDevice.address, WandUtils.setRelayFormat(1))) {
                wandDevice.relay = 1;
                Database.mWandDeviceDao.updateDevice(wandDevice);
            }
        }
    }

    @Override
    public void onRefresh() {

        for (String address : mFoundedDevicesAddresses) {
            if (mPairedDevicesMap.containsKey(address)) {
                mAdapter.notifyDeviceFounded(address);
            }
        }

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mScanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            scanLeDevice(true);
        }

        mRefreshDevices.setRefreshing(false);
    }

    public void setVisibleLayout() {
        boolean value = mAdapter.getItemCount() <= 0;
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
            mLeScanner.startScan(null, mScanSettings, mScanCallback);
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

            if (address != null && mPairedDevicesMap.containsKey(address) && !mFoundedDevicesAddresses.contains(address)) {
                if (mBluetoothLeService != null) {
                    if (mBluetoothLeService.connect(address)) {
                        mFoundedDevicesAddresses.add(address);
                    }
                }
            }
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

    private void processData(String address, String data) {
        WandDevice device = mPairedDevicesMap.get(address);
        switch (data) {
            case WandAttributes.DETECT_NEW_CONNECTION:
                if (mBluetoothLeService != null && device != null)
                    mBluetoothLeService.writeCharacteristic(address, WandUtils.setEnterPasswordFormat(device.password));
                break;
            case WandAttributes.AUTOMATIC_LOCK:
                if (device != null) {
                    device.relay = 1;
                    Database.mWandDeviceDao.updateDevice(device);
                    mAdapter.update(device);
                }
                break;
            case WandAttributes.ENTER_PASSWORD_OK:
                if (address != null) {
                    if (mPairedDevicesMap != null && mPairedDevicesMap.containsKey(address)) {
                        if (mAdapter != null)
                            mAdapter.notifyDeviceFounded(address);
                    }
                    mBluetoothLeService.writeCharacteristic(address, WandUtils.getState());
                }
                break;
            default:
                if (address != null) {
                    if (data.contains("#E") && data.contains("OK@")) {
                        if (data.contains(WandAttributes.MODE_MANUAL)) {
                            device.mode = "M";
                        } else if (data.contains(WandAttributes.MODE_AUTOMATIC)) {
                            device.mode = "A";
                        }
                        if (data.contains(WandAttributes.RELAY_ENABLED)) {
                            device.relay = 1;
                        } else if (data.contains(WandAttributes.RELAY_DISABLED)) {
                            device.relay = 0;
                        }
                        if (Database.mWandDeviceDao.updateDevice(device)) {
                            mAdapter.update(device);
                            L.info("Device " + device.address + " of " + device.owner + " updated.");
                        }
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
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

    void showDeleteDeviceDialog(final int position) {
        final WandDevice wandDevice = mAdapter.get(position);

        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        mDialogBuilder.setTitle(getString(R.string.label_advertisement));
        mDialogBuilder.setMessage(getString(R.string.warning_delete_device_of_owner, wandDevice.name, wandDevice.owner));
        mDialogBuilder.setNegativeButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        mDialogBuilder.setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (mPairedDevicesMap.containsKey(wandDevice.address))
                    mPairedDevicesMap.remove(wandDevice.address);
                mBluetoothLeService.closeConnection(wandDevice.address);
                mAdapter.remove(position);
                Database.mWandDeviceDao.delete(wandDevice);
                setVisibleLayout();
            }
        });
        mDialogBuilder.show();
    }
}
