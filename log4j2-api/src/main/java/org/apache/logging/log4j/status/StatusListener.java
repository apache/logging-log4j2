package org.apache.logging.log4j.status;

/**
 * Interface that allows implementors to be notified of events in the logging system.
 */
public interface StatusListener {

    /**
     * Called as events occur to process the StatusData.
     * @param data The StatusData for the event.
     */
    void log(StatusData data);
}
