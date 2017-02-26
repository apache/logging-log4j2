package org.apache.logging.log4j.core.pattern;

/**
 *
 */
public final class NotANumber {
    public static final NotANumber NAN = new NotANumber();
    public static final String VALUE = "\u0000";

    private NotANumber() {
    }

    public String toString() {
        return VALUE;
    }
}
