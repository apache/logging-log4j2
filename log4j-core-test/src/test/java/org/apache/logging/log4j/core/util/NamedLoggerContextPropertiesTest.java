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
package org.apache.logging.log4j.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.selector.BasicContextSelector;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DisabledUntil;

@ContextSelectorType(NamedLoggerContextPropertiesTest.TestContextSelector.class)
@DisabledUntil(
        date = "2024-05-15",
        reason = "There is currently no property source that retrieves data from META-INF/log4j2.<context name>.json")
public class NamedLoggerContextPropertiesTest {

    @Test
    public void testProperties() {
        final LoggerContext context = LoggerContext.getContext();
        assertEquals(LifeCycle.State.STARTED, context.getState());
        final PropertyEnvironment props = context.getEnvironment();
        assertNotNull(props, "Logger Context Properties were not loaded");
        final String scriptLanguages = props.getStringProperty("Script.enableLanguages");
        assertEquals("Groovy,JavaScript", scriptLanguages);
        final Configuration config = context.getConfiguration();
        assertNotNull(config, "Configuration was not created");
        assertEquals("DSI", config.getName(), "Incorrect configuration name");
        LogManager.shutdown();
        assertEquals(LifeCycle.State.STOPPED, context.getState());
    }

    public static class TestContextSelector extends BasicContextSelector {

        @Inject
        public TestContextSelector(final ConfigurableInstanceFactory injector) {
            super(injector);
        }

        @Override
        protected LoggerContext createContext() {
            return createContext("my-app", null, getClass().getClassLoader());
        }
    }
}
