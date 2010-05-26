package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

/**
 *
 */
public interface Filter {

    public enum Result {
        ACCEPT, NEUTRAL, DENY
    }

    /**
     * @doubt lifecycle management should be orthogonal.
    */
    void start();

    /**
     * @doubt lifecycle management should be orthogonal.
    */
    void stop();

    /**
     * @doubt lifecycle management should be orthogonal.
    */
    boolean isStarted();

    Result getOnMismatch();

    Result getOnMatch();

    Result filter(Logger logger, Level level, Marker marker, String msg, Object... params);

    Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t);

    Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t);

    Result filter(LogEvent event);

}
