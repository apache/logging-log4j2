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
import java.util.Enumeration;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * <em>Consider this class private.</em>
 * <p>
 * Helps access properties.
 * </p>
 */
public final class PropertiesUtil {

    private static final PropertiesUtil LOG4J_PROPERTIES = new PropertiesUtil("log4j2.component.properties");

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Properties props;

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

    public PropertiesUtil(final String propsLocn) {
        final ClassLoader loader = LoaderUtil.getThreadContextClassLoader();
        @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
        final
        Properties properties = new Properties();
            try {
                final Enumeration<URL> enumeration = loader.getResources(propsLocn);
                while (enumeration.hasMoreElements()) {
                    final URL url = enumeration.nextElement();
                    final InputStream in = url.openStream();
                    try {
                        properties.load(in);
                    } catch (final IOException ioe) {
                        LOGGER.error("Unable to read {}", url.toString());
                    } finally {
                        try {
                            in.close();
                        } catch (final IOException ioe) {
                            LOGGER.error("Unable to close {}", url.toString(), ioe);
                        }
                    }

                }

            } catch (final IOException ioe) {
                LOGGER.error("Unable to access {}", propsLocn, ioe);
            }
        this.props = properties;
    }

    public static PropertiesUtil getProperties() {
        return LOG4J_PROPERTIES;
    }

    public String getStringProperty(final String name) {
        String prop = null;
        try {
            prop = System.getProperty(name);
        } catch (final SecurityException ignored) {
            // Ignore
        }
        return prop == null ? props.getProperty(name) : prop;
    }


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
            } catch (final Exception ex) {
                return defaultValue;
            }
        }
        return defaultValue;
    }


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
            } catch (final Exception ex) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public String getStringProperty(final String name, final String defaultValue) {
        final String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : prop;
    }

    public boolean getBooleanProperty(final String name) {
        return getBooleanProperty(name, false);
    }

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
