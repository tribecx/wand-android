package com.tunashields.wand.utils;

/**
 * Created by Irvin on 9/20/17.
 */

public class WandUtils {

    public static String setChangeNameOwnerFormat(String name, String owner) {
        return "#N" + name + "-" + owner + "@";
    }

    public static String setChangePasswordFormat(String password) {
        return "#C" + password + "@";
    }

    public static String setEnterPasswordFormat(String password) {
        return "#P" + password + "@";
    }

    public static String setRelayFormat(int enable) {
        return "#R" + String.valueOf(enable) + "@";
    }

}
