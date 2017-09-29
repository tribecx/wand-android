package com.tunashields.wand.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Irvin on 9/29/17.
 */

public class DateUtils {
    public static String DATE_FORMAT = "MM-yy";
    public static String NEW_DATE_FORMAT = "MMMM-yyyy";

    public static String setDateFormat(String date) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, new Locale("es", "US"));
        SimpleDateFormat new_format = new SimpleDateFormat(NEW_DATE_FORMAT, new Locale("es", "US"));
        try {
            Date formatted_date = format.parse(date);
            return new_format.format(formatted_date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
