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

import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for LoggerConfig.
 */
public class LoggerConfigTest {

    private static LoggerConfig createForProperties(final Property[] properties) {
        return LoggerConfig.createLogger(true, Level.INFO, "name", "false", new AppenderRef[0], properties,
                new NullConfiguration(), null);
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    @Test
    public void testPropertiesWithoutSubstitution() {
        assertNull("null propertiesList", createForProperties(null).getPropertyList());
        assertNull("null property Map", createForProperties(null).getProperties());

        final Property[] all = new Property[] {
                Property.createProperty("key1", "value1"),
                Property.createProperty("key2", "value2"),
        };
        final LoggerConfig loggerConfig = createForProperties(all);
        final List<Property> list = loggerConfig.getPropertyList();
        assertEquals("map and list contents equal", new HashSet(list), loggerConfig.getProperties().keySet());

        final Object[] actualList = new Object[1];
        loggerConfig.setLogEventFactory(new LogEventFactory() {
            @Override
            public LogEvent createEvent(final String loggerName, final Marker marker, final String fqcn,
                    final Level level, final Message data,
                    final List<Property> properties, final Throwable t) {
                actualList[0] = properties;
                return new Log4jLogEvent(System.currentTimeMillis());
            }
        });
        loggerConfig.log("name", "fqcn", null, Level.INFO, new SimpleMessage("msg"), null);
        assertSame("propertiesList passed in as is if no substitutions required", list, actualList[0]);
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    @Test
    public void testPropertiesWithSubstitution() {
        final Property[] all = new Property[] {
                Property.createProperty("key1", "value1-${sys:user.name}"),
                Property.createProperty("key2", "value2-${sys:user.name}"),
        };
        final LoggerConfig loggerConfig = createForProperties(all);
        final List<Property> list = loggerConfig.getPropertyList();
        assertEquals("map and list contents equal", new HashSet(list), loggerConfig.getProperties().keySet());

        final Object[] actualListHolder = new Object[1];
        loggerConfig.setLogEventFactory(new LogEventFactory() {
            @Override
            public LogEvent createEvent(final String loggerName, final Marker marker, final String fqcn,
                    final Level level, final Message data,
                    final List<Property> properties, final Throwable t) {
                actualListHolder[0] = properties;
                return new Log4jLogEvent(System.currentTimeMillis());
            }
        });
        loggerConfig.log("name", "fqcn", null, Level.INFO, new SimpleMessage("msg"), null);
        assertNotSame("propertiesList with substitutions", list, actualListHolder[0]);

        final List<Property> actualList = (List<Property>) actualListHolder[0];

        for (int i = 0; i < list.size(); i++) {
            assertEquals("name[" + i + "]", list.get(i).getName(), actualList.get(i).getName());
            final String value = list.get(i).getValue().replace("${sys:user.name}", System.getProperty("user.name"));
            assertEquals("value[" + i + "]", value, actualList.get(i).getValue());
        }
    }
}
