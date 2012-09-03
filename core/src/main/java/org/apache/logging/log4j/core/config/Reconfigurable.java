package org.apache.logging.log4j.core.config;

/**
 *  Interface to be implemented by Configurations that can be reconfigured at runtime.
 */
public interface Reconfigurable {

    Configuration reconfigure();
}
