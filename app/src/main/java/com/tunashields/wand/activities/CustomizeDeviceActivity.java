package com.tunashields.wand.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.tunashields.wand.R;

public class CustomizeDeviceActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    private String mDeviceName;
    private String mDeviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_device);

        Bundle extras = getIntent().getExtras();
        mDeviceName = extras.getString(EXTRA_DEVICE_NAME);
        mDeviceAddress = extras.getString(EXTRA_DEVICE_ADDRESS);
    }
}
