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
package org.apache.logging.log4j.plugins.condition;

import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled // Test still doesn't work.
class OnPropertyConditionTest {

    static class OnProperty {
        @ConditionalOnProperty(name = "foo.bar", value = "true")
        @Factory
        String truth() {
            return "truth";
        }

        @ConditionalOnProperty(name = "foo.bar")
        @Ordered(Ordered.LAST)
        @Factory
        String string() {
            return "hello";
        }

        @Ordered(10)
        @Factory
        String backup() {
            return "goodbye";
        }
    }

    @Test
    @ClearSystemProperty(key = "foo.bar")
    void whenPropertyAbsent() {
        ((PropertiesUtil) PropertiesUtil.getProperties()).reload();
        final String value = DI.createInjector(OnProperty.class).getInstance(String.class);
        assertEquals("goodbye", value);
    }

    @Test
    @SetSystemProperty(key = "foo.bar", value = "whatever")
    void whenPropertyPresent() {
        ((PropertiesUtil) PropertiesUtil.getProperties()).reload();
        final String value = DI.createInjector(OnProperty.class).getInstance(String.class);
        assertEquals("hello", value);
    }

    @Test
    @SetSystemProperty(key = "foo.bar", value = "true")
    void whenPropertyMatches() {
        ((PropertiesUtil) PropertiesUtil.getProperties()).reload();
        final String value = DI.createInjector(OnProperty.class).getInstance(String.class);
        assertEquals("truth", value);
    }
}
