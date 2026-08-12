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
package org.apache.logging.log4j.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;

class PropertyTest {

    @Test
    void testCreateProperty() {
        final Property property = Property.createProperty("name", "value");
        assertEquals("name", property.getName());
        assertEquals("value", property.getRawValue());
        assertEquals("value", property.getValue());
        assertEquals("name=value", property.toString());
    }

    @Test
    void testNullValueIsConvertedToEmptyString() {
        assertEquals("", Property.createProperty("name", null).getValue());
    }

    @Test
    void testIsValueNeedsLookup() {
        assertTrue(Property.createProperty("", "${").isValueNeedsLookup());
        assertTrue(Property.createProperty("", "blah${blah").isValueNeedsLookup());
        assertFalse(Property.createProperty("", "").isValueNeedsLookup());
        assertFalse(Property.createProperty("", "plain").isValueNeedsLookup());
    }

    @Test
    void testEqualsAndHashCode() {
        final Property property1 = Property.createProperty("name", "raw", "value");
        final Property property2 = Property.createProperty("name", "raw", "value");
        final Property property3 = Property.createProperty("other", "raw", "value");

        assertEquals(property1, property2);
        assertEquals(property1.hashCode(), property2.hashCode());
        assertNotEquals(property1, property3);
    }

    @Test
    void testSerializableRoundTrip() throws Exception {
        final Property original = Property.createProperty("name", "${ctx:foo}", "resolved");
        final Property restored = serializeRoundTrip(original);

        assertNotSame(original, restored);
        assertEquals(original, restored);
    }

    private static <T> T serializeRoundTrip(final T object) throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            @SuppressWarnings("unchecked")
            final T restored = (T) ois.readObject();
            return restored;
        }
    }
}
