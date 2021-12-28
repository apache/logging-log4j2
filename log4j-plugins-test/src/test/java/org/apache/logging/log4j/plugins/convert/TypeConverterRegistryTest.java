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

import org.apache.logging.log4j.plugins.Plugin;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;

public class TypeConverterRegistryTest {

    @Test(expected = NullPointerException.class)
    public void testFindNullConverter() {
        TypeConverterRegistry.getInstance().findCompatibleConverter(null);
    }

    @Test
    public void testFindBooleanConverter() throws Exception {
        final TypeConverter<?> converter = TypeConverterRegistry.getInstance().findCompatibleConverter(Boolean.class);
        assertNotNull(converter);
        assertTrue((Boolean) converter.convert("TRUE"));
    }

    @Test
    public void testFindPrimitiveBooleanConverter() throws Exception {
        final TypeConverter<?> converter = TypeConverterRegistry.getInstance().findCompatibleConverter(Boolean.TYPE);
        assertNotNull(converter);
        assertTrue((Boolean) converter.convert("tRUe"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindCharSequenceConverterUsingStringConverter() throws Exception {
        final TypeConverter<CharSequence> converter = (TypeConverter<CharSequence>)
            TypeConverterRegistry.getInstance().findCompatibleConverter(CharSequence.class);
        assertNotNull(converter);
        assertThat(converter, instanceOf(TypeConverters.StringConverter.class));
        final CharSequence expected = "This is a test sequence of characters";
        final CharSequence actual = converter.convert(expected.toString());
        assertEquals(expected, actual);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindNumberConverter() throws Exception {
        final TypeConverter<Number> numberTypeConverter = (TypeConverter<Number>)
            TypeConverterRegistry.getInstance().findCompatibleConverter(Number.class);
        assertNotNull(numberTypeConverter);
        // TODO: is there a specific converter this should return?
    }

    public enum Foo {
        I, PITY, THE
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindEnumConverter() throws Exception {
        final TypeConverter<Foo> fooTypeConverter = (TypeConverter<Foo>)
            TypeConverterRegistry.getInstance().findCompatibleConverter(Foo.class);
        assertNotNull(fooTypeConverter);
        assertEquals(Foo.I, fooTypeConverter.convert("i"));
        assertEquals(Foo.PITY, fooTypeConverter.convert("pity"));
        assertEquals(Foo.THE, fooTypeConverter.convert("THE"));
    }

    public static final class CustomTestClass1 {

        private CustomTestClass1() {}

    }

    @Plugin(name = "CustomTestClass1Converter1", category = TypeConverters.CATEGORY)
    public static final class CustomTestClass1Converter1
            implements TypeConverter<CustomTestClass1> {

        @Override
        public CustomTestClass1 convert(final String ignored) {
            return new CustomTestClass1();
        }

    }

    @SuppressWarnings("ComparableType")
    @Plugin(name = "CustomTestClass1Converter2", category = TypeConverters.CATEGORY)
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
        final TypeConverter<?> converter = TypeConverterRegistry
                .getInstance()
                .findCompatibleConverter(CustomTestClass1.class);
        assertThat(converter, instanceOf(CustomTestClass1Converter2.class));
    }

    public static final class CustomTestClass2 {

        private CustomTestClass2() {}

    }

    @Plugin(name = "CustomTestClass2Converter1", category = TypeConverters.CATEGORY)
    public static final class CustomTestClass2Converter1
            implements TypeConverter<CustomTestClass2> {

        @Override
        public CustomTestClass2 convert(final String ignored) {
            return new CustomTestClass2();
        }

    }

    @Plugin(name = "CustomTestClass2Converter2", category = TypeConverters.CATEGORY)
    public static final class CustomTestClass2Converter2
            implements TypeConverter<CustomTestClass2> {

        @Override
        public CustomTestClass2 convert(final String ignored) {
            return new CustomTestClass2();
        }

    }

    @Test
    public void testMultipleIncomparableConverters() {
        final TypeConverter<?> converter = TypeConverterRegistry
                .getInstance()
                .findCompatibleConverter(CustomTestClass2.class);
        assertThat(converter, instanceOf(CustomTestClass2Converter1.class));
    }

}
