package com.tunashields.wand.models;

/**
 * Created by Irvin on 8/31/17.
 */

public class WandDevice {
    public int id;
    public String address;
    public String name;
    public String owner;
    public String password;
    public String mode;
    public String relay;
    public String version;
    public String manufacturing_date;

    public WandDevice() {
    }

    public WandDevice(String address, String name, String owner, String password) {
        this.address = address;
        this.name = name;
        this.owner = owner;
        this.password = password;
    }

    public WandDevice(String address, String name, String owner, String password, String mode, String relay, String version, String manufacturing_date) {
        this.address = address;
        this.name = name;
        this.owner = owner;
        this.password = password;
        this.mode = mode;
        this.relay = relay;
        this.version = version;
        this.manufacturing_date = manufacturing_date;
    }

    public WandDevice(int id, String address, String name, String owner, String password, String mode, String relay, String version, String manufacturing_date) {
        this.id = id;
        this.address = address;
        this.name = name;
        this.owner = owner;
        this.password = password;
        this.mode = mode;
        this.relay = relay;
        this.version = version;
        this.manufacturing_date = manufacturing_date;
    }
}
