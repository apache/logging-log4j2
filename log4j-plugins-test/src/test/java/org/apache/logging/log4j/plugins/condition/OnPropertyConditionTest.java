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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
@Disabled //"https://issues.apache.org/jira/browse/LOG4J2-3521"
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

        @ConditionalOnMissingBinding
        @Factory
        String backup() {
            return "goodbye";
        }
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("foo.bar");
        PropertiesUtil.getProperties().reload();
    }

    @Test
    void whenPropertyAbsent() {
        final String value = DI.createInjector(OnProperty.class).getInstance(String.class);
        assertEquals("goodbye", value);
    }

    @Test
    void whenPropertyPresent() {
        System.setProperty("foo.bar", "whatever");
        final String value = DI.createInjector(OnProperty.class).getInstance(String.class);
        assertEquals("hello", value);
    }

    @Test
    void whenPropertyMatches() {
        System.setProperty("foo.bar", "true");
        final String value = DI.createInjector(OnProperty.class).getInstance(String.class);
        assertEquals("truth", value);
    }
}
