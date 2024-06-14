package Utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static String formatTimestamp(Date timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(timestamp);
    }
}
