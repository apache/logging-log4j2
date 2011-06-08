package org.apache.logging.log4j.core.net;

/**
 *
 */
public enum Protocol {

    TCP, UDP;

    public boolean equals(String name) {
        return this.name().equalsIgnoreCase(name);
    }
}
