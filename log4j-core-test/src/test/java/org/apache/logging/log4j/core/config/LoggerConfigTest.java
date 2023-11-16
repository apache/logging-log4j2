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
package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

/**
 * Tests for LoggerConfig.
 */
public class LoggerConfigTest {

    private static final String FQCN = LoggerConfigTest.class.getName();

    private static LoggerConfig createForProperties(final Property[] properties) {
        return LoggerConfig.createLogger(
                true, Level.INFO, "name", "false", new AppenderRef[0], properties, new NullConfiguration(), null);
    }

    @SuppressWarnings({"deprecation"})
    @Test
    public void testPropertiesWithoutSubstitution() {
        assertNull(createForProperties(null).getPropertyList(), "null propertiesList");

        final Property[] all = new Property[] {
            Property.createProperty("key1", "value1"), Property.createProperty("key2", "value2"),
        };
        final LoggerConfig loggerConfig = createForProperties(all);
        final List<Property> list = loggerConfig.getPropertyList();
        assertEquals(new HashSet<>(list), new HashSet<>(loggerConfig.getPropertyList()), "map and list contents equal");

        final AtomicReference<Object> actualList = new AtomicReference<>();
        loggerConfig.setLogEventFactory((loggerName, marker, fqcn, level, data, properties, t) -> {
            actualList.set(properties);
            return new Builder().setTimeMillis(System.currentTimeMillis()).build();
        });
        loggerConfig.log("name", "fqcn", null, Level.INFO, new SimpleMessage("msg"), null);
        assertSame(list, actualList.get(), "propertiesList passed in as is if no substitutions required");
    }

    @Test
    public void testPropertiesWithSubstitution() {
        final Property[] all = new Property[] {
            Property.createProperty("key1", "value1-${sys:user.name}"),
            Property.createProperty("key2", "value2-${sys:user.name}"),
        };
        final LoggerConfig loggerConfig = createForProperties(all);
        final List<Property> list = loggerConfig.getPropertyList();
        assertEquals(new HashSet<>(list), new HashSet<>(loggerConfig.getPropertyList()), "map and list contents equal");

        final AtomicReference<Object> actualListHolder = new AtomicReference<>();
        loggerConfig.setLogEventFactory((loggerName, marker, fqcn, level, data, properties, t) -> {
            actualListHolder.set(properties);
            return new Builder().setTimeMillis(System.currentTimeMillis()).build();
        });
        loggerConfig.log("name", "fqcn", null, Level.INFO, new SimpleMessage("msg"), null);
        assertNotSame(list, actualListHolder.get(), "propertiesList with substitutions");

        @SuppressWarnings("unchecked")
        final List<Property> actualList = (List<Property>) actualListHolder.get();

        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i).getName(), actualList.get(i).getName(), "name[" + i + "]");
            final String value = list.get(i).getValue().replace("${sys:user.name}", System.getProperty("user.name"));
            assertEquals(value, actualList.get(i).getValue(), "value[" + i + "]");
        }
    }

    @Test
    public void testLevel() {
        final Configuration configuration = new DefaultConfiguration();
        final LoggerConfig config1 = LoggerConfig.newBuilder()
                .withLoggerName("org.apache.logging.log4j.test")
                .withLevel(Level.ERROR)
                .withAdditivity(false)
                .withConfig(configuration)
                .build();
        final LoggerConfig config2 = LoggerConfig.newBuilder()
                .withLoggerName("org.apache.logging.log4j")
                .withAdditivity(false)
                .withConfig(configuration)
                .build();
        config1.setParent(config2);
        assertEquals(config1.getLevel(), Level.ERROR, "Unexpected Level");
        assertEquals(config1.getExplicitLevel(), Level.ERROR, "Unexpected explicit level");
        assertEquals(config2.getLevel(), Level.ERROR, "Unexpected Level");
        assertNull(config2.getExplicitLevel(), "Unexpected explicit level");
    }

    @Test
    public void testSingleFilterInvocation() {
        final Configuration configuration = new NullConfiguration();
        final Filter filter = mock(Filter.class);
        final LoggerConfig config = LoggerConfig.newBuilder()
                .withLoggerName(FQCN)
                .withConfig(configuration)
                .withLevel(Level.INFO)
                .withtFilter(filter)
                .build();
        final Appender appender = mock(Appender.class);
        when(appender.isStarted()).thenReturn(true);
        when(appender.getName()).thenReturn("test");
        config.addAppender(appender, null, null);

        config.log(FQCN, FQCN, null, Level.INFO, new SimpleMessage(), null);
        verify(appender, times(1)).append(any());
        verify(filter, times(1)).filter(any());
    }
}
