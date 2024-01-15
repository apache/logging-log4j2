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
package org.apache.logging.log4j.core.plugins.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.junit.jupiter.api.Test;

public class CoreTypeConvertersTest {

    private final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();

    @Test
    public void testFindNullConverter() {
        assertThrows(NullPointerException.class, () -> instanceFactory.getTypeConverter(null));
    }

    @Test
    public void testFindBooleanConverter() throws Exception {
        final TypeConverter<?> converter = instanceFactory.getTypeConverter(Boolean.class);
        assertNotNull(converter);
        assertTrue((Boolean) converter.convert("TRUE"));
    }

    @Test
    public void testFindPrimitiveBooleanConverter() throws Exception {
        final TypeConverter<?> converter = instanceFactory.getTypeConverter(Boolean.TYPE);
        assertNotNull(converter);
        assertTrue((Boolean) converter.convert("tRUe"));
    }

    @Test
    public void testFindCharSequenceConverterUsingStringConverter() throws Exception {
        final TypeConverter<CharSequence> converter = instanceFactory.getTypeConverter(CharSequence.class);
        assertNotNull(converter);
        final CharSequence expected = "This is a test sequence of characters";
        final CharSequence actual = converter.convert(expected.toString());
        assertEquals(expected, actual);
    }

    @Test
    public void testFindNumberConverter() throws Exception {
        final TypeConverter<Number> numberTypeConverter = instanceFactory.getTypeConverter(Number.class);
        assertNotNull(numberTypeConverter);
        // TODO: is there a specific converter this should return?
    }

    public enum Foo {
        I,
        PITY,
        THE
    }

    @Test
    public void testFindEnumConverter() throws Exception {
        final TypeConverter<Foo> fooTypeConverter = instanceFactory.getTypeConverter(Foo.class);
        assertNotNull(fooTypeConverter);
        assertEquals(Foo.I, fooTypeConverter.convert("i"));
        assertEquals(Foo.PITY, fooTypeConverter.convert("pity"));
        assertEquals(Foo.THE, fooTypeConverter.convert("THE"));
    }
}
