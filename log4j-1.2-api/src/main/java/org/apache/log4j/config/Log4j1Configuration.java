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

import org.apache.log4j.builders.BuilderManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Reconfigurable;

/**
 * Class Description goes here.
 */
public class Log4j1Configuration extends AbstractConfiguration implements Reconfigurable {

    public static final String MONITOR_INTERVAL = "log4j1.monitorInterval";
    public static final String APPENDER_REF_TAG = "appender-ref";
    public static final String THRESHOLD_PARAM = "Threshold";

    public static final String INHERITED = "inherited";

    public static final String NULL = "null";

    protected final BuilderManager manager;

    public Log4j1Configuration(final LoggerContext loggerContext, final ConfigurationSource source,
            int monitorIntervalSeconds) {
        super(loggerContext, source);
        manager = new BuilderManager();
        initializeWatchers(this, source, monitorIntervalSeconds);
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
