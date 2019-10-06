package org.apache.logging.log4j.plugins.bind;

public class OptionBindingException extends IllegalArgumentException {

    public OptionBindingException(final String name, final Object value) {
        super("Invalid value '" + value + "' for option '" + name + "'");
    }

    public OptionBindingException(final String name, final Object value, final Throwable cause) {
        super("Unable to set option '" + name + "' to value '" + value + "'", cause);
    }

    public OptionBindingException(final String s) {
        super(s);
    }

    public OptionBindingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public OptionBindingException(final Throwable cause) {
        super(cause);
    }
}
