package org.apache.logging.log4j.core;

/**
 * Status of a life cycle like a {@link LoggerContext}..
 */
public enum LifeCycleState {
    /** Initialized but not yet started. */
    INITIALIZED,
    /** In the process of starting. */
    STARTING,
    /** Is active. */
    STARTED,
    /** Stopping is in progress. */
    STOPPING,
    /** Has stopped. */
    STOPPED
}