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
package org.apache.logging.log4j.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@LoggerContextSource("log4j-test2.xml")
public class LoggerUpdateTest {

    private final ListAppender app;

    public LoggerUpdateTest(@Named("List") final ListAppender app) {
        this.app = app.clear();
    }

    @Test
    public void resetLevel(final LoggerContext context) {
        final org.apache.logging.log4j.Logger logger = context.getLogger("com.apache.test");
        logger.traceEntry();
        List<LogEvent> events = app.getEvents();
        assertThat(events).hasSize(1);
        app.clear();
        final Configuration config = context.getConfiguration();
        final LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        /* You could also specify the actual logger name as below and it will return the LoggerConfig used by the Logger.
           LoggerConfig loggerConfig = getLoggerConfig("com.apache.test");
        */
        loggerConfig.setLevel(Level.DEBUG);
        context.updateLoggers(); // This causes all Loggers to refetch information from their LoggerConfig.
        logger.traceEntry();
        events = app.getEvents();
        assertThat(events).isEmpty();
    }

    @Test
    @Timeout(3)
    public void testUpdateLoggersPropertyListeners(final LoggerContext context) throws Exception {
        final Configuration config = context.getConfiguration();
        final Consumer<Configuration> listener = configuration -> assertSame(config, configuration);
        context.addConfigurationStartedListener(listener);
        try {
            context.updateLoggers();
        } finally {
            context.removeConfigurationStartedListener(listener);
        }
    }

    @Test
    public void testDeprecatedConstructor(final LoggerContext context) {
        try {
            Configuration configuration = context.getConfiguration();
            LoggerConfig loggerConfig = new LoggerConfig("test1", Level.INFO, false);
            assertThat(loggerConfig).isNotNull();
        } catch (Exception ex) {
            fail("Constructor should not have failed: " + ex.getMessage());
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void propertyChangeListenerReceivesOldAndNewConfigurationOnReconfiguration(final LoggerContext context) {
        // Given a java.beans listener, as registered by Log4j Core 2 consumers such as Micrometer
        final List<PropertyChangeEvent> events = new ArrayList<>();
        final PropertyChangeListener listener = events::add;
        context.addPropertyChangeListener(listener);
        final Configuration previous = context.getConfiguration();
        final Configuration replacement = new DefaultConfiguration();

        // When the configuration is replaced
        try {
            context.setConfiguration(replacement);
        } finally {
            // Restore the shared context for the other tests in this class; `previous` has been stopped,
            // so reload the original configuration file instead of reinstating it.
            context.removePropertyChangeListener(listener);
            context.reconfigure();
        }

        // Then the listener sees the old and the new configuration
        assertThat(events).isNotEmpty();
        final PropertyChangeEvent last = events.get(events.size() - 1);
        assertThat(last.getSource()).isSameAs(context);
        assertThat(last.getPropertyName()).isEqualTo(LoggerContext.PROPERTY_CONFIG);
        assertThat(last.getOldValue()).isSameAs(previous);
        assertThat(last.getNewValue()).isSameAs(replacement);
    }

    @Test
    @SuppressWarnings("deprecation")
    void removedPropertyChangeListenerIsNotNotified(final LoggerContext context) {
        // Given a listener that was registered and then removed
        final List<PropertyChangeEvent> events = new ArrayList<>();
        final PropertyChangeListener listener = events::add;
        context.addPropertyChangeListener(listener);
        context.removePropertyChangeListener(listener);

        // When the loggers are updated
        context.updateLoggers();

        // Then the listener is not called
        assertThat(events).isEmpty();
    }

    @Test
    @SuppressWarnings("deprecation")
    void nullPropertyChangeListenerIsRejected(final LoggerContext context) {
        assertThatThrownBy(() -> context.addPropertyChangeListener(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("listener");
    }
}
