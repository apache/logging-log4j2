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

import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to help with accessing System Properties.
 */
public class PropertiesUtil {

    private static final PropertiesUtil LOG4J_PROPERTIES = new PropertiesUtil("log4j2.component.properties");

    private final Properties props;

    public PropertiesUtil(final Properties props) {
        this.props = props;
    }

    public PropertiesUtil(final String propsLocn) {
        this.props = new Properties();
        final ClassLoader loader = ProviderUtil.findClassLoader();
        final InputStream in = loader.getResourceAsStream(propsLocn);
        if (null != in) {
            try {
                this.props.load(in);
            } catch (final IOException e) {
                // ignored
            } finally {
                try {
                    in.close();
                } catch (final IOException e) {
                    // ignored
                }
            }
        }
    }

    public static PropertiesUtil getProperties() {
        return LOG4J_PROPERTIES;
    }

    public String getStringProperty(final String name) {
        String prop = null;
        try {
            prop = System.getProperty(name);
        } catch (final SecurityException e) {
            // Ignore
        }
        return (prop == null) ? props.getProperty(name) : prop;
    }


    public int getIntegerProperty(final String name, final int defaultValue) {
        String prop = null;
        try {
            prop = System.getProperty(name);
        } catch (final SecurityException e) {
            // Ignore
        }
        if (prop == null) {
            prop = props.getProperty(name);
        }
        if (prop != null) {
            try {
                return Integer.parseInt(prop);
            } catch (Exception ex) {
                return defaultValue;
            }
        }
        return defaultValue;
    }


    public long getLongProperty(final String name, final long defaultValue) {
        String prop = null;
        try {
            prop = System.getProperty(name);
        } catch (final SecurityException e) {
            // Ignore
        }
        if (prop == null) {
            prop = props.getProperty(name);
        }
        if (prop != null) {
            try {
                return Long.parseLong(prop);
            } catch (Exception ex) {
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
            StatusLogger.getLogger().error("Unable to access system properties.");
            // Sandboxed - can't read System Properties
            return new Properties();
        }
    }
}
