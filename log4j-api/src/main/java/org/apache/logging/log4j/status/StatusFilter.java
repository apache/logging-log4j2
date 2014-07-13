package org.apache.logging.log4j.status;

public interface StatusFilter {
    boolean isEnabled(StatusData data);
}
