package org.apache.logging.log4j.message;

/**
 * Messages that use this interface will cause the timestamp in the message to be used instead of the timestmap in
 * the LogEvent.
 */
public interface TimestampMessage {
    long getTimestamp();
}
