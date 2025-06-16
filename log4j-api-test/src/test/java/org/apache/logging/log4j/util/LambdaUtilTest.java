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
package org.apache.logging.log4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

/**
 * Tests the LambdaUtil class.
 */
class LambdaUtilTest {

    @Test
    void testGetSupplierResultOfSupplier() {
        final String expected = "result";
        final Object actual = LambdaUtil.get((Supplier<String>) () -> expected);
        assertSame(expected, actual);
    }

    @Test
    void testGetMessageSupplierResultOfSupplier() {
        final Message expected = new SimpleMessage("hi");
        final Message actual = LambdaUtil.get(() -> expected);
        assertSame(expected, actual);
    }

    @Test
    void testGetSupplierReturnsNullIfSupplierNull() {
        final Object actual = LambdaUtil.get((Supplier<?>) null);
        assertNull(actual);
    }

    @Test
    void testGetMessageSupplierReturnsNullIfSupplierNull() {
        final Object actual = LambdaUtil.get(null);
        assertNull(actual);
    }

    @Test
    void testGetSupplierExceptionIfSupplierThrowsException() {
        assertThrows(
                RuntimeException.class,
                () -> LambdaUtil.get((Supplier<String>) () -> {
                    throw new RuntimeException();
                }));
    }

    @Test
    void testGetMessageSupplierExceptionIfSupplierThrowsException() {
        assertThrows(
                RuntimeException.class,
                () -> LambdaUtil.get(() -> {
                    throw new RuntimeException();
                }));
    }

    @Test
    void testGetAllReturnsResultOfSuppliers() {
        final String expected1 = "result1";
        final Supplier<String> function1 = () -> expected1;
        final String expected2 = "result2";
        final Supplier<String> function2 = () -> expected2;

        final Supplier<?>[] functions = {function1, function2};
        final Object[] actual = LambdaUtil.getAll(functions);
        assertEquals(actual.length, functions.length);
        assertSame(expected1, actual[0]);
        assertSame(expected2, actual[1]);
    }

    @Test
    void testGetAllReturnsNullArrayIfSupplierArrayNull() {
        final Object[] actual = LambdaUtil.getAll((Supplier<?>[]) null);
        assertNull(actual);
    }

    @Test
    void testGetAllReturnsNullElementsIfSupplierArrayContainsNulls() {
        final Supplier<?>[] functions = new Supplier<?>[3];
        final Object[] actual = LambdaUtil.getAll(functions);
        assertEquals(actual.length, functions.length);
        for (final Object object : actual) {
            assertNull(object);
        }
    }

    @Test
    void testGetAllThrowsExceptionIfAnyOfTheSuppliersThrowsException() {
        final Supplier<String> function1 = () -> "abc";
        final Supplier<String> function2 = () -> {
            throw new RuntimeException();
        };

        final Supplier<?>[] functions = {function1, function2};
        assertThrows(RuntimeException.class, () -> LambdaUtil.getAll(functions));
    }
}
