package org.apache.logging.log4j.plugins.bind;

public class ConfigurationBindingException extends IllegalArgumentException {

    ConfigurationBindingException(final String name, final Object value) {
        super("Invalid value '" + value + "' for option '" + name + "'");
    }

    ConfigurationBindingException(final String name, final Object value, final Throwable cause) {
        super("Unable to set option '" + name + "' to value '" + value + "'", cause);
    }

    ConfigurationBindingException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
