package com.tunashields.wand.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Irvin on 8/31/17.
 */

public class WandDevice implements Parcelable {

    public static final String KEY = "wand_device";

    public int id;
    public String address;
    public String name;
    public String owner;
    public String password;
    public String mode;
    public int relay;
    public String serial_number;
    public String version;
    public String manufacturing_date;
    public boolean close;

    public WandDevice() {
    }

    public WandDevice(String address, String name, String password) {
        this.address = address;
        this.name = name;
        this.password = password;
    }

    public WandDevice(String address, String name, String owner, String password, String mode, int relay) {
        this.address = address;
        this.name = name;
        this.owner = owner;
        this.password = password;
        this.mode = mode;
        this.relay = relay;
    }

    protected WandDevice(Parcel in) {
        id = in.readInt();
        address = in.readString();
        name = in.readString();
        owner = in.readString();
        password = in.readString();
        mode = in.readString();
        relay = in.readInt();
        serial_number = in.readString();
        version = in.readString();
        manufacturing_date = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(address);
        dest.writeString(name);
        dest.writeString(owner);
        dest.writeString(password);
        dest.writeString(mode);
        dest.writeInt(relay);
        dest.writeString(serial_number);
        dest.writeString(version);
        dest.writeString(manufacturing_date);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WandDevice> CREATOR = new Creator<WandDevice>() {
        @Override
        public WandDevice createFromParcel(Parcel in) {
            return new WandDevice(in);
        }

        @Override
        public WandDevice[] newArray(int size) {
            return new WandDevice[size];
        }
    };
}
