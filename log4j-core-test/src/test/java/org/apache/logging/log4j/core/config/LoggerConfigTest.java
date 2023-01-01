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

import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.Cast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LoggerConfig.
 */
public class LoggerConfigTest {

    private static LoggerConfig createForProperties(final Property[] properties) {
        return LoggerConfig.newBuilder()
                .setAdditivity(true)
                .setLevel(Level.INFO)
                .setLoggerName("name")
                .setIncludeLocation("false")
                .setProperties(properties)
                .setConfig(new NullConfiguration())
                .get();
    }

    @Test
    public void testPropertiesWithoutSubstitution() {
        assertNull(createForProperties(null).getPropertyList(), "null propertiesList");

        final Property[] all = new Property[] {
                Property.createProperty("key1", "value1"),
                Property.createProperty("key2", "value2"),
        };
        final LoggerConfig loggerConfig = createForProperties(all);
        final List<Property> list = loggerConfig.getPropertyList();
        assertEquals(new HashSet<>(list),
                     new HashSet<>(loggerConfig.getPropertyList()), "map and list contents equal");

        final Object[] actualList = new Object[1];
        loggerConfig.setLogEventFactory((loggerName, marker, fqcn, level, data, properties, t) -> {
            actualList[0] = properties;
            return LogEvent.builder().setTimeMillis(System.currentTimeMillis()).get();
        });
        loggerConfig.log("name", "fqcn", null, Level.INFO, new SimpleMessage("msg"), null);
        assertSame(list, actualList[0], "propertiesList passed in as is if no substitutions required");
    }

    @Test
    public void testPropertiesWithSubstitution() {
        final Property[] all = new Property[] {
                Property.createProperty("key1", "value1-${sys:user.name}"),
                Property.createProperty("key2", "value2-${sys:user.name}"),
        };
        final LoggerConfig loggerConfig = createForProperties(all);
        final List<Property> list = loggerConfig.getPropertyList();
        assertEquals(new HashSet<>(list),
                     new HashSet<>(loggerConfig.getPropertyList()), "map and list contents equal");

        final Object[] actualListHolder = new Object[1];
        loggerConfig.setLogEventFactory((loggerName, marker, fqcn, level, data, properties, t) -> {
            actualListHolder[0] = properties;
            return LogEvent.builder().setTimeMillis(System.currentTimeMillis()).get();
        });
        loggerConfig.log("name", "fqcn", null, Level.INFO, new SimpleMessage("msg"), null);
        assertNotSame(list, actualListHolder[0], "propertiesList with substitutions");

        final List<Property> actualList = Cast.cast(actualListHolder[0]);

        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i).getName(), actualList.get(i).getName(), "name[" + i + "]");
            final String value = list.get(i).getValue().replace("${sys:user.name}", System.getProperty("user.name"));
            assertEquals(value, actualList.get(i).getValue(), "value[" + i + "]");
        }
    }

    @Test
    public void testLevel() {
        Configuration configuration = new DefaultConfiguration(LoggerContext.getContext());
        LoggerConfig config1 = LoggerConfig.newBuilder()
                .setLoggerName("org.apache.logging.log4j.test")
                .setLevel(Level.ERROR)
                .setAdditivity(false)
                .setConfig(configuration)
                .build();
        LoggerConfig config2 = LoggerConfig.newBuilder()
                .setLoggerName("org.apache.logging.log4j")
                .setAdditivity(false)
                .setConfig(configuration)
                .build();
        config1.setParent(config2);
        assertEquals(config1.getLevel(), Level.ERROR, "Unexpected Level");
        assertEquals(config1.getExplicitLevel(), Level.ERROR, "Unexpected explicit level");
        assertEquals(config2.getLevel(), Level.ERROR, "Unexpected Level");
        assertNull(config2.getExplicitLevel(),"Unexpected explicit level");
    }
}
