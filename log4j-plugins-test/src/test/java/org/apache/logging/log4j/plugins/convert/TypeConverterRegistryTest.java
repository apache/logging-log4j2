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

package org.apache.logging.log4j.plugins.convert;

import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

class TypeConverterRegistryTest {

    public static final class CustomTestClass1 {

        private CustomTestClass1() {}

    }

    @TypeConverters
    @Plugin
    public static final class CustomTestClass1Converter1
            implements TypeConverter<CustomTestClass1> {

        @Override
        public CustomTestClass1 convert(final String ignored) {
            return new CustomTestClass1();
        }

    }

    @SuppressWarnings("ComparableType")
    @TypeConverters
    @Plugin
    @Ordered(Ordered.FIRST)
    public static final class CustomTestClass1Converter2
            implements TypeConverter<CustomTestClass1>, Comparable<TypeConverter<?>> {

        @Override
        public CustomTestClass1 convert(final String ignored) {
            return new CustomTestClass1();
        }

        @Override
        public int compareTo(@SuppressWarnings("NullableProblems") final TypeConverter<?> converter) {
            return -1;
        }

    }

    @Test
    public void testMultipleComparableConverters() {
        final Injector injector = DI.createInjector();
        injector.init();
        final TypeConverter<?> converter = injector.getTypeConverter(CustomTestClass1.class);
        assertThat(converter, instanceOf(CustomTestClass1Converter2.class));
    }

    public static final class CustomTestClass2 {

        private CustomTestClass2() {}

    }

    @TypeConverters
    @Plugin
    public static final class CustomTestClass2Converter1
            implements TypeConverter<CustomTestClass2> {

        @Override
        public CustomTestClass2 convert(final String ignored) {
            return new CustomTestClass2();
        }

    }

    @TypeConverters
    @Plugin
    public static final class CustomTestClass2Converter2
            implements TypeConverter<CustomTestClass2> {

        @Override
        public CustomTestClass2 convert(final String ignored) {
            return new CustomTestClass2();
        }

    }

    @Test
    public void testMultipleIncomparableConverters() {
        final Injector injector = DI.createInjector();
        injector.init();
        final TypeConverter<?> converter = injector.getTypeConverter(CustomTestClass2.class);
        assertThat(converter, instanceOf(CustomTestClass2Converter1.class));
    }

}
