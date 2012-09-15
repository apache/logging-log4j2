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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import java.net.URI;

/**
 * Class that can be used to initialize and configure the Logging system.
 */
public final class Configurator {

    private Configurator() {
    }

    /**
     * Initialize the Logging Context.
     * @param name The Context name.
     * @param loader The ClassLoader for the Context (or null).
     * @param configLocation The configuration for the logging context.
     * @return The LoggerContext.
     */
    public static LoggerContext intitalize(String name, ClassLoader loader, String configLocation) {

        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(loader, false);
            URI uri = configLocation == null ? null : new URI(configLocation);
            Configuration config = ConfigurationFactory.getInstance().getConfiguration(name, uri);
            ctx.setConfiguration(config);
            return ctx;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void shutdown(LoggerContext ctx) {
        if (ctx != null) {
            ctx.setConfiguration(new DefaultConfiguration());
        }
    }

}
