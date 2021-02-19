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

package org.apache.logging.log4j.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

/**
 * Tests the LambdaUtil class.
 */
public class LambdaUtilTest {

    @Test
    public void testGetSupplierResultOfSupplier() {
        final String expected = "result";
        final Object actual = LambdaUtil.get((Supplier<String>) () -> expected);
        assertThat(actual).isSameAs(expected);
    }

    @Test
    public void testGetMessageSupplierResultOfSupplier() {
        final Message expected = new SimpleMessage("hi");
        final Message actual = LambdaUtil.get(() -> expected);
        assertThat(actual).isSameAs(expected);
    }

    @Test
    public void testGetSupplierReturnsNullIfSupplierNull() {
        final Object actual = LambdaUtil.get((Supplier<?>) null);
        assertThat(actual).isNull();
    }

    @Test
    public void testGetMessageSupplierReturnsNullIfSupplierNull() {
        final Object actual = LambdaUtil.get((MessageSupplier) null);
        assertThat(actual).isNull();
    }

    @Test
    public void testGetSupplierExceptionIfSupplierThrowsException() {
        assertThatThrownBy(() -> LambdaUtil.get((Supplier<String>) () -> {
            throw new RuntimeException();
        })).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testGetMessageSupplierExceptionIfSupplierThrowsException() {
        assertThatThrownBy(() -> LambdaUtil.get(() -> {
            throw new RuntimeException();
        })).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testGetAllReturnsResultOfSuppliers() {
        final String expected1 = "result1";
        final Supplier<String> function1 = () -> expected1;
        final String expected2 = "result2";
        final Supplier<String> function2 = () -> expected2;

        final Supplier<?>[] functions = { function1, function2 };
        final Object[] actual = LambdaUtil.getAll(functions);
        assertThat(functions.length).isEqualTo(actual.length);
        assertThat(actual[0]).isSameAs(expected1);
        assertThat(actual[1]).isSameAs(expected2);
    }

    @Test
    public void testGetAllReturnsNullArrayIfSupplierArrayNull() {
        final Object[] actual = LambdaUtil.getAll((Supplier<?>[]) null);
        assertThat(actual).isNull();
    }

    @Test
    public void testGetAllReturnsNullElementsIfSupplierArrayContainsNulls() {
        final Supplier<?>[] functions = new Supplier<?>[3];
        final Object[] actual = LambdaUtil.getAll(functions);
        assertThat(functions.length).isEqualTo(actual.length);
        for (final Object object : actual) {
            assertThat(object).isNull();
        }
    }

    @Test
    public void testGetAllThrowsExceptionIfAnyOfTheSuppliersThrowsException() {
        final Supplier<String> function1 = () -> "abc";
        final Supplier<String> function2 = () -> {
            throw new RuntimeException();
        };

        final Supplier<?>[] functions = { function1, function2 };
        assertThatThrownBy(() -> LambdaUtil.getAll(functions)).isInstanceOf(RuntimeException.class);
    }
}
