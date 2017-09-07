package com.tunashields.wand.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
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

public class MainActivity extends AppCompatActivity {

    RecyclerView mRecyclerView;
    WandDevicesAdapter mAdapter;

    private BluetoothAdapter mBluetoothAdapter;
    private final int MY_REQUEST_ENABLE_BT = 101;
    private final int MY_REQUEST_ENABLE_GPS = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_paired_devices);
        mAdapter = new WandDevicesAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        /*mAdapter.add(new WandDevice("car", "Mustang", "Irvin", false));
        mAdapter.add(new WandDevice("garage", "VMW", "Irvin", true));
        mAdapter.add(new WandDevice("garage", "House Garage", "Irvin", false));*/

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
                if (resultCode == Activity.RESULT_OK) {
                    // TODO: 9/7/17 Looking for paired devices
                } else if (resultCode == Activity.RESULT_CANCELED) {
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
        startActivity(new Intent(MainActivity.this, AddDeviceActivity.class));
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
