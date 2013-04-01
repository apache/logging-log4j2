package org.apache.logging.log4j.async;

import org.apache.logging.log4j.status.StatusLogger;

public class ClockFactory {

    public static final String PROPERTY_NAME = "AsyncLogger.Clock";
    //private static final Clock clock = createClock();

    public static Clock getClock() {
        return createClock();
    }

    private static Clock createClock() {
        final StatusLogger LOGGER = StatusLogger.getLogger();
        String userRequest = System.getProperty(PROPERTY_NAME);
        if (userRequest == null || "SystemClock".equals(userRequest)) {
            LOGGER.debug("Using default SystemClock for timestamps");
            return new SystemClock();
        }
        if ("org.apache.logging.log4j.async.CachedClock".equals(userRequest)
                || "CachedClock".equals(userRequest)) {
            LOGGER.debug("Using specified CachedClock for timestamps");
            return CachedClock.instance();
        }
        if ("org.apache.logging.log4j.async.CoarseCachedClock"
                .equals(userRequest) || "CoarseCachedClock".equals(userRequest)) {
            LOGGER.debug("Using specified CoarseCachedClock for timestamps");
            return CoarseCachedClock.instance();
        }
        try {
            Clock result = (Clock) Class.forName(userRequest).newInstance();
            LOGGER.debug("Using {} for timestamps", userRequest);
            return result;
        } catch (Exception e) {
            String fmt = "Could not create {}: {}, using default SystemClock for timestamps";
            LOGGER.error(fmt, userRequest, e);
            return new SystemClock();
        }
    }
}
