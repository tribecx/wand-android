package com.tunashields.wand.activities;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.tunashields.wand.R;
import com.tunashields.wand.adapters.WandDevicesAdapter;
import com.tunashields.wand.models.WandDevice;

public class MainActivity extends AppCompatActivity {

    RecyclerView mRecyclerView;
    WandDevicesAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_paired_devices);
        mAdapter = new WandDevicesAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.add(new WandDevice("car", "Mustang", "Irvin", false));
        mAdapter.add(new WandDevice("garage", "VMW", "Irvin", true));
        mAdapter.add(new WandDevice("garage", "House Garage", "Irvin", false));

        setVisibleLayout(mAdapter.getItemCount() <= 0);

        checkBluetooth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setVisibleLayout(boolean value) {
        findViewById(R.id.image_no_paired_devices).setVisibility(value ? View.VISIBLE : View.GONE);
        findViewById(R.id.layout_paired_devices).setVisibility(value ? View.GONE : View.VISIBLE);
    }

    public void checkBluetooth() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(this);
            mDialogBuilder.setTitle(getString(R.string.warning_disable_bluetooth_title));
            mDialogBuilder.setMessage(getString(R.string.warning_disable_bluetooth_message));
            mDialogBuilder.setNegativeButton(getString(R.string.label_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                }
            });
            mDialogBuilder.setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            mDialogBuilder.create().show();
        }
    }

    public void startAddDeviceActivity(View view) {
        startActivity(new Intent(MainActivity.this, AddDeviceActivity.class));
    }

    public void startSettingsActivity(View view) {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
    }
}
