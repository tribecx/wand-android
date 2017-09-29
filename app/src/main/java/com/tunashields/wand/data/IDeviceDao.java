package com.tunashields.wand.data;

import com.tunashields.wand.models.WandDevice;

import java.util.ArrayList;

/**
 * Created by Irvin on 9/21/17.
 */

public interface IDeviceDao {
    // add device
    public boolean addDevice(WandDevice device);

    public boolean updateDevice(WandDevice device);

    // get saved devices
    public ArrayList<WandDevice> getAllDevices();
}
