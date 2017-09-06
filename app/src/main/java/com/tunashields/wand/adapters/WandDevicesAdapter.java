package com.tunashields.wand.adapters;

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

public class WandDevicesAdapter extends RecyclerView.Adapter<WandDevicesAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<WandDevice> mWandDevices;

    public WandDevicesAdapter(Context mContext) {
        this.mContext = mContext;
        mWandDevices = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wand_device, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        WandDevice wandDevice = mWandDevices.get(position);
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

    @Override
    public int getItemCount() {
        return mWandDevices.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
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

    public void add(WandDevice wandDevice) {
        mWandDevices.add(wandDevice);
        notifyDataSetChanged();
    }

    public void addAll(ArrayList<WandDevice> wandDevices) {
        mWandDevices.addAll(wandDevices);
        notifyDataSetChanged();
    }
}
