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
package org.apache.logging.log4j.plugins.condition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.UsingTestProperties;
import org.junit.jupiter.api.Test;

@UsingTestProperties
class OnPropertyConditionTest {

    static class Default {
        @Factory
        @ConditionalOnMissingBinding
        String defaultValue() {
            return "default";
        }
    }

    static class Fixture extends Default {
        @Factory
        @ConditionalOnProperty(name = "OnPropertyConditionTest", value = "first")
        String firstValue() {
            return "one";
        }

        @Factory
        @ConditionalOnProperty(name = "OnPropertyConditionTest", value = "second")
        String secondValue() {
            return "two";
        }

        @Factory
        @ConditionalOnProperty(name = "PropertyThree")
        String thirdValue() {
            return "three";
        }
    }

    @ConditionalOnProperty(name = "Applied")
    static class AppliedToMethods {
        @Factory
        String appliedValue() {
            return "apply";
        }
    }

    final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();

    @Test
    void whenPropertyAbsent() {
        instanceFactory.registerBundle(Fixture.class);
        final String value = instanceFactory.getInstance(String.class);
        assertEquals("default", value);
    }

    @Test
    @SetTestProperty(key = "log4j.OnPropertyConditionTest", value = "first")
    void whenPropertyMatchesFirst() {
        instanceFactory.registerBundle(Fixture.class);
        final String value = instanceFactory.getInstance(String.class);
        assertEquals("one", value);
    }

    @Test
    @SetTestProperty(key = "log4j.OnPropertyConditionTest", value = "second")
    void whenPropertyMatchesSecond() {
        instanceFactory.registerBundle(Fixture.class);
        final String value = instanceFactory.getInstance(String.class);
        assertEquals("two", value);
    }

    @Test
    @SetTestProperty(key = "log4j.PropertyThree", value = "")
    void whenPropertyPresent() {
        instanceFactory.registerBundle(Fixture.class);
        final String value = instanceFactory.getInstance(String.class);
        assertEquals("three", value);
    }

    @Test
    @SetTestProperty(key = "log4j.Applied", value = "")
    void whenPropertyPresentForConditionalClass() {
        instanceFactory.registerBundle(AppliedToMethods.class);
        instanceFactory.registerBundle(Fixture.class);
        final String value = instanceFactory.getInstance(String.class);
        assertEquals("apply", value);
    }
}
