package com.tunashields.wand.models;

/**
 * Created by Irvin on 8/31/17.
 */

public class WandDevice {
    public String type;
    public String name;
    public String owner;
    public boolean locked;

    public WandDevice(String type, String name, String owner, boolean locked) {
        this.type = type;
        this.name = name;
        this.owner = owner;
        this.locked = locked;
    }
}
