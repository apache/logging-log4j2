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
package org.apache.logging.log4j.core.config.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LifeCycle.State;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests: {@link DefaultComponentBuilder}.
 */
@SuppressWarnings({"DataFlowIssue", "RedundantCast"})
class DefaultComponentBuilderTest {

    private final DefaultComponentBuilder<?, ?> builder =
            new DefaultComponentBuilder<>(ConfigurationBuilderFactory.newConfigurationBuilder(), "test");

    @BeforeEach
    void setup() {
        builder.clear();
    }

    @Test
    void testAddAttribute_DuplicateKey() {

        final String key = "k1";
        final String lastValue = "foobar";

        builder.addAttribute(key, Level.ERROR);
        builder.addAttribute(key, State.INITIALIZING);
        builder.addAttribute(key, lastValue);

        assertEquals(
                lastValue,
                builder.getAttribute(key),
                "addAttribute(String, String) should have set the attribute value.");
        assertEquals(
                1,
                builder.getAttributes().size(),
                "addAttribute(String, String) should have added the attribute only once.");
    }

    @Test
    void testAddAttribute_Bool_OK() {

        final String key = "k1";
        final boolean booleanValue = true;

        builder.addAttribute(key, booleanValue);

        assertEquals(
                Boolean.toString(booleanValue),
                builder.getAttribute(key),
                "addAttribute(String, boolean) should have set the attribute value.");
    }

    @Test
    void testAddAttribute_Bool_NullKey() {

        assertThrows(
                NullPointerException.class,
                () -> builder.addAttribute((String) null, true),
                "addAttribute(String, boolean) should throw an exception with null key");
    }

    @Test
    void testAddAttribute_Int_OK() {

        final String key = "k1";
        final int value = 5;

        builder.addAttribute(key, value);

        assertEquals(
                Integer.toString(value),
                builder.getAttribute(key),
                "addAttribute(String, int) should have set the attribute value.");
    }

    @Test
    void testAddAttribute_Int_NullKey() {

        assertThrows(
                NullPointerException.class,
                () -> builder.addAttribute((String) null, 5),
                "addAttribute(String, int) should throw an exception with null key");
    }

    @Test
    void testAddAttribute_Enum_OK() {

        final String key = "k1";
        final State enumValue = State.INITIALIZING;

        builder.addAttribute(key, enumValue);

        assertEquals(
                enumValue.name(),
                builder.getAttribute(key),
                "addAttribute(String, Enum) should have set the attribute value.");
    }

    @Test
    void testAddAttribute_Enum_NullKey() {

        assertThrows(
                NullPointerException.class,
                () -> builder.addAttribute((String) null, State.INITIALIZING),
                "addAttribute(String, Enum) should throw an exception with null key");
    }

    @Test
    void testAddAttribute_Enum_NullValue() {

        final String key = "k1";

        builder.addAttribute(key, "foobar");
        builder.addAttribute(key, (State) null);

        assertFalse(
                builder.getAttributes().containsKey(key),
                "addAttribute(String, Enum) should remove the attribute with a null value");
    }

    @Test
    void testAddAttribute_Level_OK() {

        final String key = "k1";
        final Level levelValue = Level.ERROR;

        builder.addAttribute(key, levelValue);

        assertEquals(
                levelValue.toString(),
                builder.getAttribute(key),
                "addAttribute(String, Level) should have set the attribute value.");
    }

    @Test
    void testAddAttribute_Level_NullKey() {

        assertThrows(
                NullPointerException.class,
                () -> builder.addAttribute((String) null, Level.ERROR),
                "addAttribute(String, Level) should throw an exception with null key");
    }

    @Test
    void testAddAttribute_Level_NullValue() {

        final String key = "k1";

        builder.addAttribute(key, "foobar");
        builder.addAttribute(key, (Level) null);

        assertFalse(
                builder.getAttributes().containsKey(key),
                "addAttribute(String, Level) should remove the attribute with a null value");
    }

    @Test
    void testAddAttribute_Object_OK() {

        final String key = "k1";
        final Long objectValue = 50L;

        builder.addAttribute(key, objectValue);

        assertEquals(
                objectValue.toString(),
                builder.getAttribute(key),
                "addAttribute(String, Object) should have set the attribute value.");
    }

    @Test
    void testAddAttribute_Object_NullKey() {

        assertThrows(
                NullPointerException.class,
                () -> builder.addAttribute((String) null, 50L),
                "addAttribute(String, Object) should throw an exception with null key");
    }

    @Test
    void testAddAttribute_Object_NullValue() {

        final String key = "k1";

        builder.addAttribute(key, "foobar");
        builder.addAttribute(key, (Long) null);

        assertFalse(
                builder.getAttributes().containsKey(key),
                "addAttribute(String, Object) should remove the attribute with a null value");
    }

    @Test
    void testAddAttribute_String_OK() {

        final String key = "k1";
        final String stringValue = "foobar";

        builder.addAttribute(key, stringValue);

        assertEquals(
                stringValue,
                builder.getAttribute(key),
                "addAttribute(String, String) should have set the attribute value.");
    }

    @Test
    void testAddAttribute_String_NullKey() {

        assertThrows(
                NullPointerException.class,
                () -> builder.addAttribute((String) null, "foobar"),
                "addAttribute(String, Enum) should throw an exception with null key");
    }

    @Test
    void testAddAttribute_String_NullValue() {

        final String key = "k1";

        builder.addAttribute(key, "foobar");
        builder.addAttribute(key, (String) null);

        assertFalse(
                builder.getAttributes().containsKey(key),
                "addAttribute(String, Enum) should remove the attribute with a null value");
    }

    @Test
    void testConstructor_NullBuilder() {
        assertThrows(NullPointerException.class, () -> new DefaultComponentBuilder<>(null, "test"));
    }

    @Test
    void testConstructor_NullType() {

        ConfigurationBuilder<?> configurationBuilder = ConfigurationBuilderFactory.newConfigurationBuilder();

        assertThrows(NullPointerException.class, () -> new DefaultComponentBuilder<>(configurationBuilder, null));
    }
}
