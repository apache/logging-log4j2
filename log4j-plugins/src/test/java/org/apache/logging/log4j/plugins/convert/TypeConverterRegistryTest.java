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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;

import org.junit.Test;

public class TypeConverterRegistryTest {

    @Test(expected = NullPointerException.class)
    public void testFindNullConverter() throws Exception {
        TypeConverterRegistry.getInstance().findCompatibleConverter(null);
    }

    @Test
    public void testFindBooleanConverter() throws Exception {
        final TypeConverter<?> converter = TypeConverterRegistry.getInstance().findCompatibleConverter(Boolean.class);
        assertThat(converter).isNotNull();
        assertThat((Boolean) converter.convert("TRUE")).isTrue();
    }

    @Test
    public void testFindPrimitiveBooleanConverter() throws Exception {
        final TypeConverter<?> converter = TypeConverterRegistry.getInstance().findCompatibleConverter(Boolean.TYPE);
        assertThat(converter).isNotNull();
        assertThat((Boolean) converter.convert("tRUe")).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindCharSequenceConverterUsingStringConverter() throws Exception {
        final TypeConverter<CharSequence> converter = (TypeConverter<CharSequence>)
            TypeConverterRegistry.getInstance().findCompatibleConverter(CharSequence.class);
        assertThat(converter).isNotNull();
        assertThat(converter).isInstanceOf(TypeConverters.StringConverter.class);
        final CharSequence expected = "This is a test sequence of characters";
        final CharSequence actual = converter.convert(expected.toString());
        assertThat(actual).isEqualTo(expected);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindNumberConverter() throws Exception {
        final TypeConverter<Number> numberTypeConverter = (TypeConverter<Number>)
            TypeConverterRegistry.getInstance().findCompatibleConverter(Number.class);
        assertThat(numberTypeConverter).isNotNull();
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
        assertThat(fooTypeConverter).isNotNull();
        assertThat(fooTypeConverter.convert("i")).isEqualTo(Foo.I);
        assertThat(fooTypeConverter.convert("pity")).isEqualTo(Foo.PITY);
        assertThat(fooTypeConverter.convert("THE")).isEqualTo(Foo.THE);
    }
}
