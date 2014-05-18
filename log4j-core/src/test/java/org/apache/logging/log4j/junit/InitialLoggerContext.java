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
package org.apache.logging.log4j.junit;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit {@link TestRule} for constructing a new LoggerContext using a specified configuration file.
 */
public class InitialLoggerContext implements TestRule {

    private final String configLocation;

    private LoggerContext context;

    public InitialLoggerContext(final String configLocation) {
        this.configLocation = configLocation;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                context = Configurator.initialize(
                        description.getDisplayName(),
                        description.getTestClass().getClassLoader(),
                        configLocation
                );
                try {
                    base.evaluate();
                } finally {
                    Configurator.shutdown(context);
                    StatusLogger.getLogger().reset();
                }
            }
        };
    }

    public LoggerContext getContext() {
        return context;
    }

    public Logger getLogger(final String name) {
        return context.getLogger(name);
    }

    public Configuration getConfiguration() {
        return context.getConfiguration();
    }

    public Appender getAppender(final String name) {
        return getConfiguration().getAppenders().get(name);
    }
}
