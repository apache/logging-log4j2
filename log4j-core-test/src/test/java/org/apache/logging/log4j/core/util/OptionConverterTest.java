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

import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link OptionConverter}.
 */
public class OptionConverterTest {

    @Test
    public void testSubstVars() {
        final Properties props = new Properties();
        props.setProperty("key", "${key}");
        props.setProperty("testKey", "Log4j");
        assertEquals("Value of key is ${key}.", OptionConverter.substVars("Value of key is ${key}.", props));
        assertEquals("Value of key is .", OptionConverter.substVars("Value of key is ${key2}.", props));
        assertEquals(
                "Value of testKey:testKey is Log4j:Log4j",
                OptionConverter.substVars("Value of testKey:testKey is ${testKey}:${testKey}", props));
    }

    /**
     * StrSubstitutor would resolve ${key} to Key, append the result to "test" and then resolve ${testKey}.
     * Verify that substVars doesn't construct dynamic keys.
     */
    @Test
    public void testAppend() {
        final Properties props = new Properties();
        props.setProperty("key", "Key");
        props.setProperty("testKey", "Hello");
        assertEquals("Value of testKey is }", OptionConverter.substVars("Value of testKey is ${test${key}}", props));
    }

    /**
     * StrSubstitutor would resolve ${key}, append the result to "test" and then resolve ${testKey}.
     * Verify that substVars will treat the second expression up to the first '}' as part of the key.
     */
    @Test
    public void testAppend2() {
        final Properties props = new Properties();
        props.setProperty("test${key", "Hello");
        assertEquals(
                "Value of testKey is Hello}", OptionConverter.substVars("Value of testKey is ${test${key}}", props));
    }

    @Test
    public void testRecursion() {
        final Properties props = new RecursiveProperties();
        props.setProperty("name", "Neo");
        props.setProperty("greeting", "Hello ${name}");

        final String s = props.getProperty("greeting");
        System.out.println("greeting = '" + s + "'");
    }

    private static class RecursiveProperties extends Properties {
        @Override
        public String getProperty(final String key) {
            System.out.println("getProperty for " + key);
            try {
                final String val = super.getProperty(key);
                // The following call works for log4j 2.17.0 and causes StackOverflowError for 2.17.1
                // This is because substVars change implementation in 2.17.1 to call StrSubstitutor.replace(val, props)
                // which calls props.getProperty() for EVERY property making it recursive
                return OptionConverter.substVars(val, this);
            } catch (Exception e) {
                return super.getProperty(key);
            }
        }
    }
}
