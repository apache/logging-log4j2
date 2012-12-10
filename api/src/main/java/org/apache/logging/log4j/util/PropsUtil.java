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

import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to help with accessing System Properties.
 */
public class PropsUtil {

    private final Properties props;

    public PropsUtil(final Properties props) {
        this.props = props;
    }

    public PropsUtil(final String propsLocn) {
        this.props = new Properties();
        final ClassLoader loader = findClassLoader();
        final InputStream in = loader.getResourceAsStream(propsLocn);
        if (null != in) {
            try {
                this.props.load(in);
                in.close();
            } catch(final java.io.IOException e) {
                // ignored
            }
        }
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

    public String getStringProperty(final String name, final String defaultValue) {
        final String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : prop;
    }

    public boolean getBooleanProperty(final String name, final boolean defaultValue) {
        final String prop = getStringProperty(name);
        return (prop == null) ? defaultValue : "true".equalsIgnoreCase(prop);
    }

    private static ClassLoader findClassLoader() {
        ClassLoader cl;
        if (System.getSecurityManager() == null) {
            cl = Thread.currentThread().getContextClassLoader();
        } else {
            cl = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<ClassLoader>() {
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                }
            );
        }
        if (cl == null) {
            cl = PropsUtil.class.getClassLoader();
        }

        return cl;
    }
}
