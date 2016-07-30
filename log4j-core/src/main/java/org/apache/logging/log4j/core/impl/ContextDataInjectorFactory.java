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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Factory for ContextDataInjectors.
 *
 * @see ContextDataInjector
 * @see org.apache.logging.log4j.core.ContextData
 * @see LogEvent#getContextData()
 * @since 2.7
 */
public class ContextDataInjectorFactory {

    /**
     * Returns a new {@code ContextDataInjector} instance based on the value of system property
     * {@code log4j2.ContextDataInjector}. If not value was specified this method returns a new
     * {@link ThreadContextDataInjector}.
     * <p>
     * Users may use this system property to specify the fully qualified class name of a class that implements the
     * {@code ContextDataInjector} interface.
     *
     * @return a ContextDataInjector that populates the {@code ContextData} of all {@code LogEvent} objects
     */
    public static ContextDataInjector getInjector() {
        final String className = PropertiesUtil.getProperties().getStringProperty("log4j2.ContextDataInjector");
        if (className == null) {
            return new ThreadContextDataInjector();
        }
        try {
            final Class<? extends ContextDataInjector> cls = LoaderUtil.loadClass(className).asSubclass(
                    ContextDataInjector.class);
            return cls.newInstance();
        } catch (final Exception dynamicFailed) {
            StatusLogger.getLogger().warn(
                    "Could not create ContextDataInjector for '{}', using default ThreadContextDataInjector: {}",
                    className, dynamicFailed);
            return new ThreadContextDataInjector();
        }
    }
}
