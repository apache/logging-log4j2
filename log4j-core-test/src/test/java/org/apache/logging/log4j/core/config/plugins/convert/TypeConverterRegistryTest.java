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
package org.apache.logging.log4j.core.config.plugins.convert;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TypeConverterRegistryTest {

    @Test
    public void testFindNullConverter() {
        assertThrows(NullPointerException.class, () -> {
            TypeConverterRegistry.getInstance().findCompatibleConverter(null);
        });
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
        final TypeConverter<Number> numberTypeConverter =
                (TypeConverter<Number>) TypeConverterRegistry.getInstance().findCompatibleConverter(Number.class);
        assertNotNull(numberTypeConverter);
        // TODO: is there a specific converter this should return?
    }

    public enum Foo {
        I,
        PITY,
        THE
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindEnumConverter() throws Exception {
        final TypeConverter<Foo> fooTypeConverter =
                (TypeConverter<Foo>) TypeConverterRegistry.getInstance().findCompatibleConverter(Foo.class);
        assertNotNull(fooTypeConverter);
        assertEquals(Foo.I, fooTypeConverter.convert("i"));
        assertEquals(Foo.PITY, fooTypeConverter.convert("pity"));
        assertEquals(Foo.THE, fooTypeConverter.convert("THE"));
    }
}
