package com.tunashields.wand.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.LayerDrawable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
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

    private OnItemClickListener mOnItemClickListener;
    private OnLockClickListener mOnLockClickListener;

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
        holder.mWandDeviceImageView.setImageResource(R.drawable.ic_wand_car_purple);
        holder.mWandDeviceNameView.setText(bluetoothDevice.getName());
        holder.mWandDeviceOwnerView.setText(mContext.getString(R.string.label_new_device));
        holder.mStatusDeviceButton.setVisibility(View.GONE);
    }

    private void bindWandViewHolder(final ViewHolder holder, final WandDevice wandDevice) {

        holder.mRootItemLayout.setBackgroundResource(wandDevice.close ? R.drawable.background_wand_device : R.drawable.background_wand_device_out_range);
        holder.mWandDeviceImageView.setImageResource(wandDevice.close ? R.drawable.ic_wand_car_purple : R.drawable.ic_wand_car_gray);
        holder.mSeparatorView.setBackgroundColor(wandDevice.close ? ContextCompat.getColor(mContext, R.color.purple) : ContextCompat.getColor(mContext, R.color.gray_dark));
        holder.mWandDeviceNameView.setText(wandDevice.name);
        holder.mWandDeviceNameView.setTextColor(wandDevice.close ? ContextCompat.getColor(mContext, R.color.purple) : ContextCompat.getColor(mContext, R.color.gray_dark));

        if (wandDevice.owner != null) {
            holder.mWandDeviceOwnerView.setText(mContext.getString(R.string.label_of, wandDevice.owner));
            holder.mWandDeviceOwnerView.setTextColor(wandDevice.close ? ContextCompat.getColor(mContext, R.color.purple) : ContextCompat.getColor(mContext, R.color.gray_dark));
        }

        if (!wandDevice.close) {
            holder.mStatusDeviceButton.setBackgroundResource(R.drawable.background_gray_borders_button);
            holder.mStatusDeviceButton.setText("");
            return;
        }

        if (wandDevice.relay == 1) {
            Resources resources = mContext.getResources();
            int vertical_margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, resources.getDisplayMetrics());
            int horizontal_margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, resources.getDisplayMetrics());

            LayerDrawable layerDrawable = (LayerDrawable) mContext.getDrawable(R.drawable.background_locked_device_button);
            if (layerDrawable != null && layerDrawable.getDrawable(1) != null)
                layerDrawable.setLayerInset(1, horizontal_margin, vertical_margin, horizontal_margin, vertical_margin);

            holder.mStatusDeviceButton.setBackground(layerDrawable);
            holder.mStatusDeviceButton.setText("");
        } else {
            if (wandDevice.mode != null && wandDevice.mode.equals("A")) {
                holder.mStatusDeviceButton.setBackgroundResource(R.drawable.background_automatic_lock_button);
                holder.mStatusDeviceButton.setText(mContext.getString(R.string.label_automatic_lock));
                holder.mStatusDeviceButton.setTextColor(ContextCompat.getColor(mContext, R.color.text_color_gray_dark));
            } else if (wandDevice.relay == 0) {
                holder.mStatusDeviceButton.setBackgroundResource(R.drawable.background_green_borders_button);
                holder.mStatusDeviceButton.setText(mContext.getString(R.string.label_lock));
                holder.mStatusDeviceButton.setTextColor(ContextCompat.getColor(mContext, R.color.text_color_green));
            }
        }

        holder.mStatusDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnLockClickListener != null) {
                    mOnLockClickListener.onLock(holder.getAdapterPosition(), wandDevice);
                }
            }
        });
    }

    private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ConstraintLayout mRootItemLayout;
        ImageView mWandDeviceImageView;
        View mSeparatorView;
        TextView mWandDeviceNameView;
        TextView mWandDeviceOwnerView;
        Button mStatusDeviceButton;

        ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mRootItemLayout = itemView.findViewById(R.id.item_wand_device);
            mWandDeviceImageView = itemView.findViewById(R.id.image_wand_device_type);
            mSeparatorView = itemView.findViewById(R.id.separator);
            mWandDeviceNameView = itemView.findViewById(R.id.text_wand_device_name);
            mWandDeviceOwnerView = itemView.findViewById(R.id.text_wand_device_owner);
            mStatusDeviceButton = itemView.findViewById(R.id.button_wand_device_state);
        }

        @Override
        public void onClick(View view) {
            if (mOnItemClickListener != null) {
                if (mItems.get(getAdapterPosition()) instanceof WandDevice) {
                    if (((WandDevice) mItems.get(getAdapterPosition())).close)
                        mOnItemClickListener.onItemClick(getAdapterPosition(), mItems.get(getAdapterPosition()));
                } else
                    mOnItemClickListener.onItemClick(getAdapterPosition(), mItems.get(getAdapterPosition()));
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

    public void update(int position, Object object) {
        mItems.remove(position);
        mItems.add(position, object);
        notifyItemChanged(position);
    }

    public void update(WandDevice wandDevice) {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i) instanceof WandDevice) {
                if (((WandDevice) mItems.get(i)).address.equals(wandDevice.address)) {
                    ((WandDevice) mItems.get(i)).name = wandDevice.name;
                    ((WandDevice) mItems.get(i)).owner = wandDevice.owner;
                    ((WandDevice) mItems.get(i)).password = wandDevice.password;
                    ((WandDevice) mItems.get(i)).mode = wandDevice.mode;
                    ((WandDevice) mItems.get(i)).relay = wandDevice.relay;
                    ((WandDevice) mItems.get(i)).version = wandDevice.version;
                    ((WandDevice) mItems.get(i)).firmware = wandDevice.firmware;
                    ((WandDevice) mItems.get(i)).manufacturing_date = wandDevice.manufacturing_date;
                    notifyItemChanged(i);
                }
            }
        }
    }

    public WandDevice get(int position) {
        return (WandDevice) this.mItems.get(position);
    }

    public void remove(int position) {
        this.mItems.remove(position);
        notifyItemRemoved(position);
    }

    public void notifyDeviceFounded(String address) {
        for (int i = 0; i < mItems.size(); i++) {
            WandDevice item = (WandDevice) mItems.get(i);
            if (item.address.equals(address)) {
                item.close = true;
                notifyItemChanged(i);
            }
        }
    }

    public void notifyDeviceDisconnected(String address) {
        for (int i = 0; i < mItems.size(); i++) {
            WandDevice item = (WandDevice) mItems.get(i);
            if (item.address.equals(address)) {
                item.close = false;
                notifyItemChanged(i);
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position, Object object);
    }

    public interface OnLockClickListener {
        void onLock(int position, WandDevice wandDevice);
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public void setOnLockClickListener(OnLockClickListener mOnLockClickListener) {
        this.mOnLockClickListener = mOnLockClickListener;
    }
}
