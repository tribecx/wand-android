package com.tunashields.wand.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.tunashields.wand.R;
import com.tunashields.wand.bluetooth.BluetoothLeService;
import com.tunashields.wand.bluetooth.WandAttributes;
import com.tunashields.wand.data.Database;
import com.tunashields.wand.models.WandDevice;
import com.tunashields.wand.utils.L;
import com.tunashields.wand.utils.WandUtils;

public class DeviceDetailActivity extends AppCompatActivity {

    private WandDevice mWandDevice;

    private Button mLockDeviceButton;
    private Switch mAutomaticModeView;

    private ProgressDialog mProgressDialog = null;
    private boolean isPasswordEntered = false;

    private String mNewName = null;
    private String mNewOwner = null;
    private String mNewPassword = null;

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

        showProgress(getString(R.string.label_connecting));
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

    private void processData(String data) {
        switch (data) {
            case WandAttributes.DETECT_NEW_CONNECTION:
                enterPassword();
                break;
            case WandAttributes.ENTER_PASSWORD_OK:
                isPasswordEntered = true;
                setUpViews();
                updateUI();
                dismissProgress();
                break;
            case WandAttributes.ENABLE_RELAY_OK:
                mWandDevice.relay = 1;
                updateUI();
                updateDB();
                dismissProgress();
                break;
            case WandAttributes.DISABLE_RELAY_OK:
                mWandDevice.relay = 0;
                updateUI();
                updateDB();
                dismissProgress();
                break;
            case WandAttributes.AUTOMATIC_MODE_OK:
                mWandDevice.mode = "A";
                updateUI();
                updateDB();
                dismissProgress();
                break;
            case WandAttributes.MANUAL_MODE_OK:
                mWandDevice.mode = "M";
                updateUI();
                updateDB();
                dismissProgress();
                break;
            case WandAttributes.CHANGE_NAME_OK:
                if (mNewName != null) {
                    mWandDevice.name = mNewName;
                    mNewName = null;
                } else if (mNewOwner != null) {
                    mWandDevice.owner = mNewOwner;
                    mNewOwner = null;
                }
                updateUI();
                updateDB();
                dismissProgress();
                break;
            case WandAttributes.CHANGE_PASSWORD_OK:
                mWandDevice.password = mNewPassword;
                mNewPassword = null;
                updateUI();
                updateDB();
                dismissProgress();
                onResume();
                break;
            case WandAttributes.CHANGE_PASSWORD_ERROR:
                mNewPassword = null;
                dismissProgress();
                Toast.makeText(DeviceDetailActivity.this, getString(R.string.error_updating_password), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void enterPassword() {
        mBluetoothLeService.writeCharacteristic(WandUtils.setEnterPasswordFormat(mWandDevice.password));
    }

    private void setUpViews() {
        mLockDeviceButton = (Button) findViewById(R.id.button_lock);
        mAutomaticModeView = (Switch) findViewById(R.id.switch_automatic_mode);
    }

    private void updateUI() {
        if (mWandDevice.mode != null && mWandDevice.mode.equals("A")) {
            mAutomaticModeView.setChecked(true);

            mLockDeviceButton.setText(getString(R.string.label_automatic_lock));
            mLockDeviceButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.text_color_gray_dark));
            mLockDeviceButton.setBackgroundResource(R.drawable.background_automatic_lock_button);
            mLockDeviceButton.setClickable(false);

        } else {
            mAutomaticModeView.setChecked(false);

            mLockDeviceButton.setClickable(true);

            if (mWandDevice.relay == 0) {
                mLockDeviceButton.setBackgroundResource(R.drawable.background_green_borders_button);
                mLockDeviceButton.setText(getString(R.string.label_lock));
                mLockDeviceButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.text_color_green));
            } else {
                Resources resources = getResources();
                int vertical_margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, resources.getDisplayMetrics());
                int horizontal_margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, resources.getDisplayMetrics());

                LayerDrawable layerDrawable = (LayerDrawable) getDrawable(R.drawable.background_locked_device_button);
                if (layerDrawable != null && layerDrawable.getDrawable(1) != null)
                    layerDrawable.setLayerInset(1, horizontal_margin, vertical_margin, horizontal_margin, vertical_margin);

                mLockDeviceButton.setText("");
                mLockDeviceButton.setBackground(layerDrawable);
            }
        }

        ((TextView) findViewById(R.id.text_wand_device_name)).setText(mWandDevice.name);
        ((TextView) findViewById(R.id.text_wand_device_owner)).setText(mWandDevice.owner != null ? mWandDevice.owner : "");
        ((TextView) findViewById(R.id.text_wand_device_password)).setText(mWandDevice.password != null ? mWandDevice.password : "00000");
        ((TextView) findViewById(R.id.text_wand_device_serial_number)).setText(mWandDevice.serial_number != null ? mWandDevice.serial_number : "");
        ((TextView) findViewById(R.id.text_wand_device_version)).setText(mWandDevice.version != null ? mWandDevice.version : "");
        ((TextView) findViewById(R.id.text_wand_device_manufacturing_date)).setText(mWandDevice.manufacturing_date != null ? mWandDevice.manufacturing_date : "");
    }

    private void updateDB() {
        if (Database.mWandDeviceDao.updateDevice(mWandDevice)) {
            L.info("Record updated");
            setResult(Activity.RESULT_OK);
        }
    }

    public void onClickLockDevice(View view) {
        if (mWandDevice.mode != null && mWandDevice.mode.equals("A"))
            return;

        showProgress(getString(R.string.label_sending));

        if (mWandDevice.relay == 0) {
            mBluetoothLeService.writeCharacteristic(WandUtils.setRelayFormat(1));
        } else {
            mBluetoothLeService.writeCharacteristic(WandUtils.setRelayFormat(0));
        }
    }

    public void onClickAutomaticMode(View view) {
        showProgress(getString(R.string.label_sending));

        if (mWandDevice.mode != null && mWandDevice.mode.equals("A")) {
            mBluetoothLeService.writeCharacteristic(WandUtils.setChangeModeFormat("M"));
        } else {
            mBluetoothLeService.writeCharacteristic(WandUtils.setChangeModeFormat("A"));
        }
    }

    public void onClickChangeName(View view) {
        showChangeNameDialog();
    }

    private void showChangeNameDialog() {
        L.info("Showing change name dialog");

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_change_name, null);

        final EditText mNameView = view.findViewById(R.id.edit_change_name);

        final AlertDialog dialog = new AlertDialog.Builder(DeviceDetailActivity.this)
                .setView(view)
                .setNeutralButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton(getString(R.string.label_ok), null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                Button button = ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        mNameView.setError(null);

                        mNewName = mNameView.getText().toString();

                        if (TextUtils.isEmpty(mNewName)) {
                            mNameView.setError(getString(R.string.error_empty_field));
                            mNewName = null;
                            return;
                        }

                        sendNewName();
                        dialogInterface.dismiss();
                    }
                });
            }
        });

        dialog.show();
    }

    private void sendNewName() {
        showProgress(getString(R.string.label_sending));
        if (mWandDevice.owner != null)
            mBluetoothLeService.writeCharacteristic(WandUtils.setChangeNameAndOwnerFormat(mNewName, mWandDevice.owner));
        else
            mBluetoothLeService.writeCharacteristic(WandUtils.setChangeNameAndOwnerFormat(mNewName, ""));
    }

    public void onClickChangeOwner(View view) {
        showChangeOwnerDialog();
    }

    private void showChangeOwnerDialog() {
        L.info("Showing change owner dialog");

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_change_owner, null);

        final EditText mOwnerView = view.findViewById(R.id.edit_change_owner);

        final AlertDialog dialog = new AlertDialog.Builder(DeviceDetailActivity.this)
                .setView(view)
                .setNeutralButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton(getString(R.string.label_ok), null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                Button button = ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        mOwnerView.setError(null);

                        mNewOwner = mOwnerView.getText().toString();

                        if (TextUtils.isEmpty(mNewOwner)) {
                            mOwnerView.setError(getString(R.string.error_empty_field));
                            mNewOwner = null;
                            return;
                        }

                        sendNewOwner();
                        dialogInterface.dismiss();
                    }
                });
            }
        });

        dialog.show();
    }

    private void sendNewOwner() {
        showProgress(getString(R.string.label_sending));
        mBluetoothLeService.writeCharacteristic(WandUtils.setChangeNameAndOwnerFormat(mWandDevice.name, mNewOwner));
    }

    public void onClickChangePassword(View view) {
        showChangePasswordDialog();
    }

    private void showChangePasswordDialog() {
        L.info("Showing change password dialog");

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_change_password, null);

        final EditText mPasswordView = view.findViewById(R.id.edit_change_password);
        final EditText mConfirmPasswordView = view.findViewById(R.id.edit_confirm_password);

        final AlertDialog dialog = new AlertDialog.Builder(DeviceDetailActivity.this)
                .setView(view)
                .setNeutralButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton(getString(R.string.label_ok), null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                Button button = ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        mPasswordView.setError(null);
                        mConfirmPasswordView.setError(null);

                        String password = mPasswordView.getText().toString();
                        String confirm_password = mConfirmPasswordView.getText().toString();

                        if (TextUtils.isEmpty(password)) {
                            mPasswordView.setError(getString(R.string.error_empty_field));
                            return;
                        } else if (TextUtils.isEmpty(confirm_password)) {
                            mConfirmPasswordView.setError(getString(R.string.error_empty_field));
                            return;
                        }

                        if (!password.equals(confirm_password)) {
                            mConfirmPasswordView.setError(getString(R.string.error_passwords_not_match));
                            return;
                        }

                        mNewPassword = password;
                        sendPassword();
                        dialogInterface.dismiss();
                    }
                });
            }
        });

        dialog.show();
    }

    private void sendPassword() {
        showProgress(getString(R.string.label_sending));
        mBluetoothLeService.writeCharacteristic(WandUtils.setChangePasswordFormat(mNewPassword));
    }

    private void showProgress(String message) {
        mProgressDialog = ProgressDialog.show(DeviceDetailActivity.this, null, message);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
    }

    private void dismissProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
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
