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
package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Log4j Constants.
 */
@Deprecated(forRemoval = true) // TODO(ms): finish migrating config values from here
public final class Constants {

    public static final String JNDI_PREFIX = "log4j2.enableJndi";
    private static final String JNDI_MANAGER_CLASS = "org.apache.logging.log4j.jndi.JndiManager";

    /**
     * Check to determine if the JNDI feature is available.
     * @param subKey The feature to check.
     * @return true if the feature is available.
     */
    private static boolean isJndiEnabled(final String subKey) {
        // FIXME(ms): check updated property names
        return PropertiesUtil.getProperties().getBooleanProperty(JNDI_PREFIX + subKey, false)
                && isClassAvailable(JNDI_MANAGER_CLASS);
    }

    public static boolean JNDI_CONTEXT_SELECTOR_ENABLED = isJndiEnabled("ContextSelector");

    public static boolean JNDI_JMS_ENABLED = isJndiEnabled("Jms");

    public static boolean JNDI_LOOKUP_ENABLED = isJndiEnabled("Lookup");

    public static boolean JNDI_JDBC_ENABLED = isJndiEnabled("Jdbc");

    /**
     * Name of the system property to use to identify the ContextSelector Class.
     */
    public static final String LOG4J_CONTEXT_SELECTOR = Log4jProperties.CONTEXT_SELECTOR_CLASS_NAME;

    /**
     * Property name for the default status (internal log4j logging) level to use if not specified in configuration.
     */
    public static final String LOG4J_DEFAULT_STATUS_LEVEL = Log4jProperties.STATUS_DEFAULT_LEVEL;

    /**
     * JNDI context name string literal.
     */
    public static final String JNDI_CONTEXT_NAME = "java:comp/env/log4j/context-name";

    /**
     * Determines if a named Class can be loaded or not.
     *
     * @param className The class name.
     * @return {@code true} if the class could be found or {@code false} otherwise.
     */
    private static boolean isClassAvailable(final String className) {
        try {
            return LoaderUtil.loadClass(className) != null;
        } catch (final Throwable e) {
            return false;
        }
    }

    /**
     * Prevent class instantiation.
     */
    private Constants() {
    }
}
