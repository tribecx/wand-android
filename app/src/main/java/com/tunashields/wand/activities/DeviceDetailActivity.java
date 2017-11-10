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
import android.os.Handler;
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
import com.tunashields.wand.utils.DateUtils;
import com.tunashields.wand.utils.L;
import com.tunashields.wand.utils.WandUtils;

public class DeviceDetailActivity extends AppCompatActivity {

    private WandDevice mWandDevice;

    private Button mLockDeviceButton;
    private Switch mAutomaticModeView;

    private ProgressDialog mProgressDialog = null;

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
            if (mWandDevice.version == null) {
                getVersion();
            } else if (mWandDevice.manufacturing_date == null) {
                getManufacturingDate();
            } else {
                getState();
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
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        getState();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mBluetoothLeService.disconnect(mWandDevice.address);
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
                if (mBluetoothLeService != null && mWandDevice != null)
                    mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.setEnterPasswordFormat(mWandDevice.password));
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
            case WandAttributes.AUTOMATIC_LOCK:
                mWandDevice.relay = 1;
                updateUI();
                updateDB();
                break;
            case WandAttributes.MANUAL_MODE_OK:
                mWandDevice.mode = "M";
                updateUI();
                updateDB();
                dismissProgress();
                getState();
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
                mBluetoothLeService.disconnect(mWandDevice.address);
                mBluetoothLeService.closeConnection(mWandDevice.address);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothLeService.connect(mWandDevice.address);
                    }
                }, 6000);
                dismissProgress();
                showProgress(getString(R.string.label_connecting), true);
                break;
            case WandAttributes.CHANGE_PASSWORD_OK:
                mWandDevice.password = mNewPassword;
                mNewPassword = null;
                updateUI();
                updateDB();
                mBluetoothLeService.disconnect(mWandDevice.address);
                mBluetoothLeService.closeConnection(mWandDevice.address);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothLeService.connect(mWandDevice.address);
                    }
                }, 6000);
                dismissProgress();
                showProgress(getString(R.string.label_connecting), true);
                break;
            case WandAttributes.CHANGE_PASSWORD_ERROR:
                mNewPassword = null;
                dismissProgress();
                Toast.makeText(DeviceDetailActivity.this, getString(R.string.error_updating_password), Toast.LENGTH_SHORT).show();
                break;
            case WandAttributes.ENTER_PASSWORD_OK:
                dismissProgress();
                break;
            default:
                if (data.contains("#V:")) {
                    L.debug(data);
                    String[] separated = data.split(",");
                    String version = separated[0].substring(3);
                    String firmware = separated[1].substring(3, separated[1].length() - 1);
                    L.debug(version);
                    L.debug(firmware);
                    mWandDevice.version = version;
                    mWandDevice.firmware = firmware;
                    Database.mWandDeviceDao.updateDevice(mWandDevice);
                    updateUI();
                    getManufacturingDate();
                }
                if (data.contains("#F:")) {
                    L.debug(data.substring(3, data.length() - 1));
                    mWandDevice.manufacturing_date = data.substring(3, data.length() - 1);
                    updateDB();
                    updateUI();
                    getState();
                }
                if (data.contains("#E") && data.contains("OK@")) {
                    if (data.contains(WandAttributes.MODE_MANUAL)) {
                        mWandDevice.mode = "M";
                    } else if (data.contains(WandAttributes.MODE_AUTOMATIC)) {
                        mWandDevice.mode = "A";
                    }

                    if (data.contains(WandAttributes.RELAY_ENABLED)) {
                        mWandDevice.relay = 1;
                    } else if (data.contains(WandAttributes.RELAY_DISABLED)) {
                        mWandDevice.relay = 0;
                    }
                    updateDB();
                    updateUI();
                }
                break;
        }

        Intent intent = new Intent();
        intent.putExtra(WandDevice.KEY, mWandDevice);
        setResult(Activity.RESULT_OK, intent);
    }

    private void setUpViews() {
        mLockDeviceButton = (Button) findViewById(R.id.button_lock);
        mAutomaticModeView = (Switch) findViewById(R.id.switch_automatic_mode);
    }

    private void updateUI() {
        if (mWandDevice.relay == 1) {
            Resources resources = getResources();
            int vertical_margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, resources.getDisplayMetrics());
            int horizontal_margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, resources.getDisplayMetrics());

            LayerDrawable layerDrawable = (LayerDrawable) getDrawable(R.drawable.background_locked_device_button);
            if (layerDrawable != null && layerDrawable.getDrawable(1) != null)
                layerDrawable.setLayerInset(1, horizontal_margin, vertical_margin, horizontal_margin, vertical_margin);

            mLockDeviceButton.setText("");
            mLockDeviceButton.setBackground(layerDrawable);

        } else {
            if (mWandDevice.mode != null && mWandDevice.mode.equals("A")) {
                mLockDeviceButton.setText(getString(R.string.label_automatic_lock));
                mLockDeviceButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.text_color_gray_dark));
                mLockDeviceButton.setBackgroundResource(R.drawable.background_automatic_lock_button);

            } else if (mWandDevice.relay == 0) {
                mLockDeviceButton.setBackgroundResource(R.drawable.background_green_borders_button);
                mLockDeviceButton.setText(getString(R.string.label_lock));
                mLockDeviceButton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.text_color_green));
            }
        }

        if (mWandDevice.mode != null && mWandDevice.mode.equals("A"))
            mAutomaticModeView.setChecked(true);
        else
            mAutomaticModeView.setChecked(false);

        ((TextView) findViewById(R.id.text_wand_device_name)).setText(mWandDevice.name);
        ((TextView) findViewById(R.id.text_wand_device_owner)).setText(mWandDevice.owner != null ? mWandDevice.owner : "");
        ((TextView) findViewById(R.id.text_wand_device_password)).setText(mWandDevice.password != null ? mWandDevice.password : "00000");
        ((TextView) findViewById(R.id.text_wand_device_version)).setText(mWandDevice.version != null ? mWandDevice.version : "");
        ((TextView) findViewById(R.id.text_wand_device_firmware)).setText(mWandDevice.firmware != null ? mWandDevice.firmware : "");

        if (mWandDevice.manufacturing_date != null) {
            String manufacturing_date = DateUtils.setDateFormat(mWandDevice.manufacturing_date);
            if (manufacturing_date != null) {
                manufacturing_date = manufacturing_date.substring(0, 1).toUpperCase() + manufacturing_date.substring(1);
                ((TextView) findViewById(R.id.text_wand_device_manufacturing_date)).setText(manufacturing_date);
            }
        }
    }

    private void updateDB() {
        if (Database.mWandDeviceDao.updateDevice(mWandDevice)) {
            L.info("Record updated");
            setResult(Activity.RESULT_OK);
        }
    }

    private void getState() {
        if (mBluetoothLeService != null)
            mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.getState());
    }

    private void getVersion() {
        mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.getVersion());
    }

    private void getManufacturingDate() {
        mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.getManufacturingDate());
    }

    public void onClickLockDevice(View view) {
        if (mWandDevice.mode != null && mWandDevice.mode.equals("A") && mWandDevice.relay == 0)
            return;

        showProgress(getString(R.string.label_sending), false);

        if (mWandDevice.relay == 0) {
            mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.setRelayFormat(1));
        } else {
            mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.setRelayFormat(0));
        }
    }

    public void onClickAutomaticMode(View view) {
        showProgress(getString(R.string.label_sending), false);

        if (mWandDevice.mode != null && mWandDevice.mode.equals("A")) {
            mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.setChangeModeFormat("M"));
        } else {
            mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.setChangeModeFormat("A"));
        }
    }

    public void onClickChangeName(View view) {
        if (mWandDevice.is_owner)
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
        showProgress(getString(R.string.label_sending), false);
        if (mWandDevice.owner != null)
            mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.setChangeNameAndOwnerFormat(mNewName, mWandDevice.owner));
        else
            mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.setChangeNameAndOwnerFormat(mNewName, ""));
    }

    public void onClickChangeOwner(View view) {
        if (mWandDevice.is_owner)
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
        showProgress(getString(R.string.label_sending), false);
        mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.setChangeNameAndOwnerFormat(mWandDevice.name, mNewOwner));
    }

    public void onClickChangePassword(View view) {
        if (mWandDevice.is_owner)
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
        showProgress(getString(R.string.label_sending), false);
        mBluetoothLeService.writeCharacteristic(mWandDevice.address, WandUtils.setChangePasswordFormat(mNewPassword));
    }

    private void showProgress(String message, boolean cancelable) {
        mProgressDialog = ProgressDialog.show(DeviceDetailActivity.this, null, message);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(cancelable);
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
