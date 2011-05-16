package org.apache.logging.log4j.core.pattern;

/**
 *
 */
public interface PatternConverter {
    /**
     * Formats an object into a string buffer.
     *
     * @param obj        event to format, may not be null.
     * @param toAppendTo string buffer to which the formatted event will be appended.  May not be null.
     */
    void format(Object obj, StringBuilder toAppendTo);

    String getName();

    String getStyleClass(Object e);
}
