/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * <em>Consider this class private.</em>
 * <p>
 * Helps access properties. This utility provides a method to override system properties by specifying properties
 * in a properties file.
 * </p>
 */
public final class PropertiesUtil {

    private static final PropertiesUtil LOG4J_PROPERTIES = new PropertiesUtil("log4j2.component.properties");

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Properties props;

    /**
     * Constructs a PropertiesUtil using a given Properties object as its source of defined properties.
     *
     * @param props the Properties to use by default
     */
    public PropertiesUtil(final Properties props) {
        this.props = props;
    }

    /**
     * Loads and closes the given property input stream.
     * If an error occurs, log to the status logger.
     *
     * @param in
     *            a property input stream.
     * @param source
     *            a source object describing the source, like a resource string
     *            or a URL.
     * @return a new Properties object
     */
    static Properties loadClose(final InputStream in, final Object source) {
        final Properties props = new Properties();
        if (null != in) {
            try {
                props.load(in);
            } catch (final IOException e) {
                LOGGER.error("Unable to read {}", source, e);
            } finally {
                try {
                    in.close();
                } catch (final IOException e) {
                    LOGGER.error("Unable to close {}", source, e);
                }
            }
        }
        return props;
    }

    /**
     * Constructs a PropertiesUtil for a given properties file name on the classpath. The properties specified in this
     * file are used by default. If a property is not defined in this file, then the equivalent system property is
     * used.
     *
     * @param propertiesFileName the location of properties file to load
     */
    public PropertiesUtil(final String propertiesFileName) {
        @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
        final Properties properties = new Properties();
        for (final URL url : LoaderUtil.findResources(propertiesFileName)) {
            InputStream in = null;
            try {
                in = url.openStream();
                properties.load(in);
            } catch (final IOException ioe) {
                LOGGER.error("Unable to read {}", url.toString(), ioe);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (final IOException ioe) {
                        LOGGER.error("Unable to close {}", url.toString(), ioe);
                    }
                }
            }
        }
        this.props = properties;
    }

    /**
     * Returns the PropertiesUtil used by Log4j.
     *
     * @return the main Log4j PropertiesUtil instance.
     */
    public static PropertiesUtil getProperties() {
        return LOG4J_PROPERTIES;
    }

    /**
     * Gets the named property as a String.
     *
     * @param name the name of the property to look up
     * @return the String value of the property or {@code null} if undefined.
     */
    public String getStringProperty(final String name) {
        String prop = null;
        try {
            prop = System.getProperty(name);
        } catch (final SecurityException ignored) {
            // Ignore
        }
        return prop == null ? props.getProperty(name) : prop;
    }

    /**
     * Gets the named property as an integer.
     *
     * @param name         the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the parsed integer value of the property or {@code defaultValue} if it was undefined or could not be
     * parsed.
     */
    public int getIntegerProperty(final String name, final int defaultValue) {
        String prop = null;
        try {
            prop = System.getProperty(name);
        } catch (final SecurityException ignored) {
            // Ignore
        }
        if (prop == null) {
            prop = props.getProperty(name);
        }
        if (prop != null) {
            try {
                return Integer.parseInt(prop);
            } catch (final Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets the named property as a long.
     *
     * @param name         the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the parsed long value of the property or {@code defaultValue} if it was undefined or could not be
     * parsed.
     */
    public long getLongProperty(final String name, final long defaultValue) {
        String prop = null;
        try {
            prop = System.getProperty(name);
        } catch (final SecurityException ignored) {
            // Ignore
        }
        if (prop == null) {
            prop = props.getProperty(name);
        }
        if (prop != null) {
            try {
                return Long.parseLong(prop);
            } catch (final Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets the named property as a String.
     *
     * @param name         the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the String value of the property or {@code defaultValue} if undefined.
     */
    public String getStringProperty(final String name, final String defaultValue) {
        final String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : prop;
    }

    /**
     * Gets the named property as a boolean value. If the property matches the string {@code "true"} (case-insensitive),
     * then it is returned as the boolean value {@code true}. Any other non-{@code null} text in the property is
     * considered {@code false}.
     *
     * @param name the name of the property to look up
     * @return the boolean value of the property or {@code false} if undefined.
     */
    public boolean getBooleanProperty(final String name) {
        return getBooleanProperty(name, false);
    }

    /**
     * Gets the named property as a boolean value.
     *
     * @param name         the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the boolean value of the property or {@code defaultValue} if undefined.
     */
    public boolean getBooleanProperty(final String name, final boolean defaultValue) {
        final String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : "true".equalsIgnoreCase(prop);
    }

    /**
     * Return the system properties or an empty Properties object if an error occurs.
     * @return The system properties.
     */
    public static Properties getSystemProperties() {
        try {
            return new Properties(System.getProperties());
        } catch (final SecurityException ex) {
            LOGGER.error("Unable to access system properties.", ex);
            // Sandboxed - can't read System Properties
            return new Properties();
        }
    }
}
