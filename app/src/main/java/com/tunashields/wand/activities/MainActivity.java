package com.tunashields.wand.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.tunashields.wand.R;
import com.tunashields.wand.adapters.WandDevicesAdapter;
import com.tunashields.wand.data.Database;
import com.tunashields.wand.models.WandDevice;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private WandDevicesAdapter mAdapter;

    private BluetoothAdapter mBluetoothAdapter;
    private final int MY_REQUEST_ENABLE_BT = 101;
    private final int MY_REQUEST_ENABLE_GPS = 102;

    private final int MY_CONFIGURE_DEVICE_REQUEST = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_paired_devices);
        mAdapter = new WandDevicesAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.addAll(Database.mWandDeviceDao.getAllDevices());
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
            public void onLock(String address, boolean isLocked) {
                if (isLocked) {
                    //unlock(address);
                } else {
                    //lock(address);
                }
            }
        });

        setVisibleLayout(mAdapter.getItemCount() <= 0);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (checkGPS()) {
            checkBluetooth();
        }
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
