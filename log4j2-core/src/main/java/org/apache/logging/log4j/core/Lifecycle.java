package org.apache.logging.log4j.core;

/**
 *
 */
public interface Lifecycle {
    void start();

    void stop();

    boolean isStarted();
}
