/*
 * Copyright (c) 2016 Nextiva, Inc. to Present.
 * All rights reserved.
 */
package org.apache.logging.log4j.spi;

/**
 * Interface to be implemented by LoggerContext's that provide a shutdown method.
 * @since 2.6
 */
public interface Terminable {

    /**
     * Requests that the logging implementation shut down.
     *
     * This call is synchronous and will block until shut down is complete.
     * This may include flushing pending log events over network connections.
     */
    void terminate();
}
