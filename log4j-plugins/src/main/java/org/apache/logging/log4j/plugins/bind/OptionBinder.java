package org.apache.logging.log4j.plugins.bind;

public interface OptionBinder {
    Object bindString(final Object target, final String value);

    Object bindObject(final Object target, final Object value);
}
