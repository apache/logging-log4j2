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
package org.apache.log4j.config;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.log4j.builders.BuilderManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.PropertiesLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

/**
 * Log4j 1 Configuration.
 */
public class Log4j1Configuration extends AbstractConfiguration implements Reconfigurable {

    public static final String MONITOR_INTERVAL = "log4j1.monitorInterval";
    public static final String APPENDER_REF_TAG = "appender-ref";
    public static final String THRESHOLD_PARAM = "Threshold";

    public static final String INHERITED = "inherited";

    public static final String NULL = "null";

    protected final BuilderManager manager = new BuilderManager();

    public Log4j1Configuration(final LoggerContext loggerContext, final ConfigurationSource configurationSource, final int monitorIntervalSeconds) {
        super(loggerContext, configurationSource);
        initializeWatchers(this, configurationSource, monitorIntervalSeconds);
    }

    public BuilderManager getBuilderManager() {
        return manager;
    }

    /**
     * Initialize the configuration.
     */
    @Override
    public void initialize() {
        getStrSubstitutor().setConfiguration(this);
        final StrSubstitutor strSubstitutor = getConfigurationStrSubstitutor();
        strSubstitutor.setConfiguration(this);
        // Start: Load system properties as default properties.
        final StrLookup variableResolver = strSubstitutor.getVariableResolver();
        if (variableResolver instanceof Interpolator) {
//            final Interpolator interpolator = (Interpolator) variableResolver;
//            final StrLookup defaultLookup = interpolator.getDefaultLookup();
//            if (defaultLookup instanceof PropertiesLookup) {
//                final Map<String, String> properties = ((PropertiesLookup) defaultLookup).getProperties();
//                final Properties systemProperties = System.getProperties();
//                // Must lock to avoid edits from other threads
//                synchronized (systemProperties) {
//                    // Merge, don't replace!
//                    systemProperties.forEach((k, v) -> properties.merge(Objects.toString(k), Objects.toString(v), (o, n) -> o));
//                }
//            }
        }
        // End: Load system properties as default properties.
        super.getScheduler().start();
        doConfigure();
        setState(State.INITIALIZED);
        LOGGER.debug("Configuration {} initialized", this);
    }

    @Override
    public Configuration reconfigure() {
        return null;
    }
}
