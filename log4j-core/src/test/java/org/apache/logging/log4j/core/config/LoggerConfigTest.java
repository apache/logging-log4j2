package org.apache.logging.log4j.core.config;/*
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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LoggerConfig.
 */
public class LoggerConfigTest {

    private static LoggerConfig createForProperties(final Property[] properties) {
        return LoggerConfig.createLogger(true, Level.INFO, "name", "false", new AppenderRef[0], properties,
                new NullConfiguration(), null);
    }

    @SuppressWarnings({"deprecation"})
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
        assertEquals(new HashSet<>(list),
        		     new HashSet<>(loggerConfig.getPropertyList()), "map and list contents equal");

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
}
