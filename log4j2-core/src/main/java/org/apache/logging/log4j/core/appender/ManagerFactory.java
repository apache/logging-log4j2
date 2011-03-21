package org.apache.logging.log4j.core.appender;

/**
 *
 */
public interface ManagerFactory<F, T> {

    F createManager(T data);
}
