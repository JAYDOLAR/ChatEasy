package Utility;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerUtil {

    private static final Logger logger = Logger.getLogger(LoggerUtil.class.getName());

    public static void logError(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public static void logErrors(String message, String throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public static void logInfo(String message) {
        logger.log(Level.INFO, message);
    }

    public static void logDebug(String message) {
        logger.log(Level.FINE, message); // FINE is closer to DEBUG in most frameworks
    }

    // You can add more logging methods for different levels (warning, etc.)
}
