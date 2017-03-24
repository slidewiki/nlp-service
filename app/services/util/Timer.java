package services.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Convienience class to get the current date and time.
 * @author aschlaf
 *
 */
public class Timer {

 
	private Timer() {
    }

    /**
     * Returns the current date and time
     * @return The current date and time
     */
    public static String getDateAndTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

}

