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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link KeyValuePair}.
 */
class KeyValuePairTest {

    @Test
    void testRejectsNullOrEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> new KeyValuePair(null, "v"));
        assertThrows(IllegalArgumentException.class, () -> new KeyValuePair("", "v"));
        assertThrows(
                IllegalArgumentException.class,
                () -> KeyValuePair.newBuilder().setValue("v").build());
        assertThrows(
                IllegalArgumentException.class,
                () -> KeyValuePair.newBuilder().setKey("").build());
    }

    @Test
    void testPreservesKeyAndValue() {
        final KeyValuePair pair = new KeyValuePair("extraField", "extraValue");
        assertEquals("extraField", pair.getKey());
        assertEquals("extraValue", pair.getValue());

        final KeyValuePair built = KeyValuePair.newBuilder()
                .setKey("extraField")
                .setValue("extraValue")
                .build();
        assertEquals("extraField", built.getKey());
        assertEquals("extraValue", built.getValue());
    }

    @Test
    void testAcceptsNullValueAndWhitespaceKey() {
        final KeyValuePair nullValue = new KeyValuePair("k", null);
        assertEquals("k", nullValue.getKey());
        assertNull(nullValue.getValue());

        final KeyValuePair built =
                KeyValuePair.newBuilder().setKey("k").setValue(null).build();
        assertEquals("k", built.getKey());
        assertNull(built.getValue());

        final KeyValuePair space = new KeyValuePair(" ", "v");
        assertEquals(" ", space.getKey());
        assertEquals("v", space.getValue());
    }

    @Test
    void testKeylessPairIsOmittedFromGelfLayout() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Configuration status=\"OFF\" name=\"KeyValuePairTest\">"
                + "<Appenders>"
                + "<List name=\"List\">"
                + "<GelfLayout host=\"myhost\" compressionType=\"OFF\">"
                + "<KeyValuePair value=\"bar\"/>"
                + "</GelfLayout>"
                + "</List>"
                + "</Appenders>"
                + "<Loggers>"
                + "<Root level=\"info\">"
                + "<AppenderRef ref=\"List\"/>"
                + "</Root>"
                + "</Loggers>"
                + "</Configuration>";
        final byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        try (final LoggerContext context = new LoggerContext("KeyValuePairTest")) {
            final XmlConfiguration configuration =
                    new XmlConfiguration(context, new ConfigurationSource(new ByteArrayInputStream(bytes)));
            context.start(configuration);
            final ListAppender list = configuration.getAppender("List");
            assertNotNull(list);
            assertTrue(list.isStarted());
            context.getLogger(KeyValuePairTest.class).info("hello");
            assertEquals(1, list.getMessages().size());
            final String payload = list.getMessages().get(0);
            assertTrue(payload.contains("\"short_message\":\"hello\""));
            assertFalse(payload.contains("bar"));
            assertFalse(payload.contains("null"));
        }
    }
}
