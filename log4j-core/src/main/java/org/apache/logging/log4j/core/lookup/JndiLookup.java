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
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Looks up keys from JNDI resources.
 */
@Plugin(name = "jndi", category = StrLookup.CATEGORY)
public class JndiLookup extends AbstractLookup {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String RESULT = "JNDI is not supported";
    /**
     * Looks up the value of the JNDI resource.
     * @param event The current LogEvent (is ignored by this StrLookup).
     * @param key  the JNDI resource name to be looked up, may be null
     * @return The String value of the JNDI resource.
     */
    @Override
    public String lookup(final LogEvent event, final String key) {
        LOGGER.warn("Attempt to use JNDI Lookup");
        return RESULT;
    }
}
