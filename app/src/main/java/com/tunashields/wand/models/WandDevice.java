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
    public String version;
    public String firmware;
    public String manufacturing_date;
    public boolean is_owner;
    public boolean close;

    public WandDevice() {
    }

    public WandDevice(String address, String name, String password, boolean is_owner) {
        this.address = address;
        this.name = name;
        this.password = password;
        this.is_owner = is_owner;
    }

    public WandDevice(String address, String name, String owner, String password, String mode, int relay, boolean is_owner) {
        this.address = address;
        this.name = name;
        this.owner = owner;
        this.password = password;
        this.mode = mode;
        this.relay = relay;
        this.is_owner = is_owner;
    }

    protected WandDevice(Parcel in) {
        id = in.readInt();
        address = in.readString();
        name = in.readString();
        owner = in.readString();
        password = in.readString();
        mode = in.readString();
        relay = in.readInt();
        version = in.readString();
        firmware = in.readString();
        manufacturing_date = in.readString();
        is_owner = in.readByte() != 0;
        close = in.readByte() != 0;
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
        dest.writeString(version);
        dest.writeString(firmware);
        dest.writeString(manufacturing_date);
        dest.writeByte((byte) (is_owner ? 1 : 0));
        dest.writeByte((byte) (close ? 1 : 0));
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
