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
package org.apache.logging.log4j.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

/**
 * Tests basic condition processing.
 */
@UsingStatusListener
public class SpringProfileTest {

    private static final String CONFIG = "log4j2-springProfile.xml";
    private static final MockEnvironment env = new MockEnvironment();
    private static final String[] DEV_PROFILES = {"dev", "staging"};

    private void registerSpringEnvironment(final LoggerContext loggerContext, final Environment env) {
        loggerContext.putObject(Log4j2SpringBootLoggingSystem.ENVIRONMENT_KEY, env);
    }

    private void clearSpringEnvironment(final LoggerContext loggerContext) {
        loggerContext.removeObject(Log4j2SpringBootLoggingSystem.ENVIRONMENT_KEY);
    }

    private void testAppenderOut(
            final LoggerContext loggerContext, final Class<? extends Appender> clazz, final String patternPrefix) {
        final Appender app = loggerContext.getConfiguration().getAppender("Out");
        assertThat(app).isInstanceOf(clazz);
        final Layout<?> layout = app.getLayout();
        assertThat(layout).isInstanceOf(PatternLayout.class);
        assertThat(((PatternLayout) layout).getConversionPattern()).startsWith(patternPrefix);
    }

    @Test
    @LoggerContextSource(CONFIG)
    void prodTest(final LoggerContext loggerContext) {
        testAppenderOut(loggerContext, ListAppender.class, "none:");
        registerSpringEnvironment(loggerContext, env);
        try {
            env.setActiveProfiles("prod");
            loggerContext.reconfigure();
            testAppenderOut(loggerContext, ListAppender.class, "prod:");
        } finally {
            clearSpringEnvironment(loggerContext);
        }
    }

    @Test
    @LoggerContextSource(CONFIG)
    void devTest(final LoggerContext loggerContext) {
        testAppenderOut(loggerContext, ListAppender.class, "none:");
        registerSpringEnvironment(loggerContext, env);
        try {
            for (final String profile : DEV_PROFILES) {
                env.setActiveProfiles(profile);
                loggerContext.reconfigure();
                testAppenderOut(loggerContext, ConsoleAppender.class, "dev:");
            }
        } finally {
            clearSpringEnvironment(loggerContext);
        }
    }
}
