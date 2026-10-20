package com.yourname.safereport.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper for formatting timestamps into readable date strings.
 */
public class DateTimeUtils {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    public static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return "Just now";
        }
        return DATE_FORMAT.format(new Date(timestamp));
    }
}
