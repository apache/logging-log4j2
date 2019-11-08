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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Reconfigurable;

/**
 * Class Description goes here.
 */
public class Log4j1Configuration extends AbstractConfiguration implements Reconfigurable {

    public Log4j1Configuration(final LoggerContext loggerContext, final ConfigurationSource source,
            int monitorIntervalSeconds) {
        super(loggerContext, source);
        initializeWatchers(this, source, monitorIntervalSeconds);
    }

    @Override
    protected void doConfigure() {
        super.getScheduler().start();

    }

    /**
     * Initialize the configuration.
     */
    @Override
    public void initialize() {
        doConfigure();
        setState(State.INITIALIZED);
        LOGGER.debug("Configuration {} initialized", this);
    }

    @Override
    public Configuration reconfigure() {
        return null;
    }
}
