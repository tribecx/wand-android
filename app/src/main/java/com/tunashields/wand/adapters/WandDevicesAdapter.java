package com.tunashields.wand.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tunashields.wand.R;
import com.tunashields.wand.models.WandDevice;

import java.util.ArrayList;

/**
 * Created by Irvin on 8/31/17.
 */

public class WandDevicesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OWN_DEVICE = 0;
    private static final int TYPE_NEW_DEVICE = 1;

    private Context mContext;
    private ArrayList<Object> mItems;

    public WandDevicesAdapter(Context mContext) {
        this.mContext = mContext;
        mItems = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wand_device, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case TYPE_OWN_DEVICE:
                bindWandViewHolder((ViewHolder) holder, (WandDevice) mItems.get(position));
                break;
            case TYPE_NEW_DEVICE:
                bindWandViewHolder((ViewHolder) holder, (BluetoothDevice) mItems.get(position));
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object item = mItems.get(position);
        if (item instanceof WandDevice) {
            return TYPE_OWN_DEVICE;
        } else if (item instanceof BluetoothDevice) {
            return TYPE_NEW_DEVICE;
        }
        throw new IllegalArgumentException("Unknown view type");
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    private void bindWandViewHolder(ViewHolder holder, BluetoothDevice bluetoothDevice) {
        holder.mWandDeviceImageView.setImageResource(bluetoothDevice.getName().equals("Wand-Auto\r\n") ? R.drawable.ic_wand_car_purple : R.drawable.ic_wand_garage_purple);
        holder.mWandDeviceNameView.setText(bluetoothDevice.getName());
        holder.mWandDeviceOwnerView.setText(mContext.getString(R.string.label_new_device));
        holder.mStatusDeviceButton.setVisibility(View.GONE);
    }

    private void bindWandViewHolder(ViewHolder holder, WandDevice wandDevice) {
        holder.mWandDeviceImageView.setImageResource(wandDevice.type.equals("car") ? R.drawable.ic_wand_car_purple : R.drawable.ic_wand_garage_purple);
        holder.mWandDeviceNameView.setText(wandDevice.name);
        holder.mWandDeviceOwnerView.setText(mContext.getString(R.string.label_of, wandDevice.owner));
        holder.mStatusDeviceButton.setBackgroundResource(wandDevice.locked ? R.drawable.background_locked_device_button : R.drawable.background_green_borders_button);
        if (wandDevice.locked) {
            holder.mStatusDeviceButton.setText("");
        } else {
            holder.mStatusDeviceButton.setText(wandDevice.type.equals("car") ? R.string.label_lock : R.string.label_activate);
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mWandDeviceImageView;
        TextView mWandDeviceNameView;
        TextView mWandDeviceOwnerView;
        Button mStatusDeviceButton;

        ViewHolder(View itemView) {
            super(itemView);
            mWandDeviceImageView = itemView.findViewById(R.id.image_wand_device_type);
            mWandDeviceNameView = itemView.findViewById(R.id.text_wand_device_name);
            mWandDeviceOwnerView = itemView.findViewById(R.id.text_wand_device_owner);
            mStatusDeviceButton = itemView.findViewById(R.id.button_wand_device_state);
        }
    }

    public void add(Object object) {
        mItems.add(object);
        notifyDataSetChanged();
    }

    public void addAll(ArrayList<WandDevice> wandDevices) {
        mItems.addAll(wandDevices);
        notifyDataSetChanged();
    }

    public boolean contains(Object object) {
        return mItems.contains(object);
    }
}
