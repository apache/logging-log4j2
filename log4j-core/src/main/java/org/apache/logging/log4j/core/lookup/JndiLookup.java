/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.lookup;

import java.util.Objects;
import javax.naming.NamingException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.net.JndiManager;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Looks up keys from JNDI resources.
 */
@Plugin(name = "jndi", category = StrLookup.CATEGORY)
public class JndiLookup extends AbstractLookup {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Marker LOOKUP = MarkerManager.getMarker("LOOKUP");

    /** JNDI resource path prefix used in a J2EE container */
    static final String CONTAINER_JNDI_RESOURCE_PATH_PREFIX = "java:comp/env/";

    /**
     * Constructs a new instance or throw IllegalStateException if this feature is disabled.
     */
    public JndiLookup() {
        if (!JndiManager.isJndiLookupEnabled()) {
            throw new IllegalStateException("JNDI must be enabled by setting log4j2.enableJndiLookup=true");
        }
    }

    /**
     * Looks up the value of the JNDI resource.
     *
     * @param event The current LogEvent (is ignored by this StrLookup).
     * @param key the JNDI resource name to be looked up, may be null
     * @return The String value of the JNDI resource.
     */
    @Override
    public String lookup(final LogEvent event, final String key) {
        if (key == null) {
            return null;
        }
        final String jndiName = convertJndiName(key);
        try (final JndiManager jndiManager = JndiManager.getDefaultManager()) {
            return Objects.toString(jndiManager.lookup(jndiName), null);
        } catch (final NamingException e) {
            LOGGER.warn(LOOKUP, "Error looking up JNDI resource [{}].", jndiName, e);
            return null;
        }
    }

    /**
     * Convert the given JNDI name to the actual JNDI name to use. Default implementation applies the "java:comp/env/"
     * prefix unless other scheme like "java:" is given.
     *
     * @param jndiName The name of the resource.
     * @return The fully qualified name to look up.
     */
    private String convertJndiName(final String jndiName) {
        if (!jndiName.startsWith(CONTAINER_JNDI_RESOURCE_PATH_PREFIX) && jndiName.indexOf(':') == -1) {
            return CONTAINER_JNDI_RESOURCE_PATH_PREFIX + jndiName;
        }
        return jndiName;
    }
}
