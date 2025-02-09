package dev.jkopecky.draftbook_backend;

import java.util.logging.Logger;

public class Log {

    private static final Logger logger = Logger.getLogger(Log.class.getName());

    public static void create(String message, String source, String severity, Throwable causingException) {
        switch (severity) {
            case "error": logger.severe("Error in " + source + " : " + message); break;
            case "warn": logger.warning("Warning in " + source + " : " + message); break;
            case "info": logger.info("Info from " + source + " : " + message); break;
            default: logger.fine("Debug from " + source + " : " + message); break;
        }
        if (causingException != null) {
            logger.finest("Logging stack trace...");
            causingException.printStackTrace();
        }
    }
}
