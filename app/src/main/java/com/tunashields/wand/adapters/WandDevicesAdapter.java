package com.tunashields.wand.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tunashields.wand.R;
import com.tunashields.wand.bluetooth.WandAttributes;
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

    OnItemClickListener mOnItemClickListener;
    OnLockClickListener mOnLockClickListener;

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
        holder.mWandDeviceImageView.setImageResource(bluetoothDevice.getName().equals(WandAttributes.CAR_DEFAULT_NAME) ? R.drawable.ic_wand_car_purple : R.drawable.ic_wand_garage_purple);
        holder.mWandDeviceNameView.setText(bluetoothDevice.getName());
        holder.mWandDeviceOwnerView.setText(mContext.getString(R.string.label_new_device));
        holder.mStatusDeviceButton.setVisibility(View.GONE);
    }

    private void bindWandViewHolder(ViewHolder holder, WandDevice wandDevice) {
        holder.mWandDeviceImageView.setImageResource(R.drawable.ic_wand_car_purple);
        holder.mWandDeviceNameView.setText(wandDevice.name);

        if (wandDevice.owner != null)
            holder.mWandDeviceOwnerView.setText(mContext.getString(R.string.label_of, wandDevice.owner));

        if (wandDevice.mode != null && wandDevice.mode.equals("A")) {
            holder.mStatusDeviceButton.setBackgroundResource(R.drawable.background_automatic_lock_button);
            holder.mStatusDeviceButton.setText(mContext.getString(R.string.label_automatic_lock));
            holder.mStatusDeviceButton.setTextColor(ContextCompat.getColor(mContext, android.R.color.darker_gray));
            return;
        }

        if (wandDevice.relay == 0) {
            holder.mStatusDeviceButton.setBackgroundResource(R.drawable.background_green_borders_button);
            holder.mStatusDeviceButton.setText(mContext.getString(R.string.label_lock));
            holder.mStatusDeviceButton.setTextColor(ContextCompat.getColor(mContext, R.color.text_color_green));
        } else if (wandDevice.relay == 1) {
            holder.mStatusDeviceButton.setBackgroundResource(R.drawable.background_locked_device_button);
            holder.mStatusDeviceButton.setText("");
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView mWandDeviceImageView;
        TextView mWandDeviceNameView;
        TextView mWandDeviceOwnerView;
        Button mStatusDeviceButton;

        ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mWandDeviceImageView = itemView.findViewById(R.id.image_wand_device_type);
            mWandDeviceNameView = itemView.findViewById(R.id.text_wand_device_name);
            mWandDeviceOwnerView = itemView.findViewById(R.id.text_wand_device_owner);
            mStatusDeviceButton = itemView.findViewById(R.id.button_wand_device_state);
        }

        @Override
        public void onClick(View view) {
            if (mOnItemClickListener != null) {
                if (mItems.get(getAdapterPosition()) instanceof WandDevice)
                    mOnItemClickListener.onItemClick((WandDevice) mItems.get(getAdapterPosition()));
                else if (mItems.get(getAdapterPosition()) instanceof BluetoothDevice)
                    mOnItemClickListener.onItemClick((BluetoothDevice) mItems.get(getAdapterPosition()));
            }
        }
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
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

    public interface OnItemClickListener {
        void onItemClick(WandDevice wandDevice);

        void onItemClick(BluetoothDevice bluetoothDevice);
    }

    public interface OnLockClickListener {
        void onLock(String address, boolean isLocked);
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public void setOnLockClickListener(OnLockClickListener mOnLockClickListener) {
        this.mOnLockClickListener = mOnLockClickListener;
    }
}
