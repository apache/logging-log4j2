package org.apache.logging.log4j.message;

public interface MessageContentFormatter {
    void formatTo(String formatString, Object[] parameters, int parameterCount, final StringBuilder buffer);
}
