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
package org.apache.logging.slf4j;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.GenericConfigurator;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.LoggerFactory;

/**
 * JUnit {@link TestRule} similar to the TestRule in {@code log4j-core} of the same name.
 *
 * @since 2.1
 * @since 2.4.1 Renamed InitialLoggerContext to LoggerContextRule
 */
public class LoggerContextRule implements TestRule {

    private final String configLocation;

    private LoggerContext context;

    private String testClassName;

    public LoggerContextRule(final String configLocation) {
        this.configLocation = configLocation;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        testClassName = description.getClassName();
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                context = (LoggerContext) LoggerFactory.getILoggerFactory();
                final GenericConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                configurator.doConfigure(configLocation);
                base.evaluate();
            }
        };
    }

    public LoggerContext getContext() {
        return context;
    }

    public Logger getLogger() {
        return context.getLogger(testClassName);
    }

    public Logger getLogger(final String name) {
        return context.getLogger(name);
    }
}
