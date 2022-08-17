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

package org.apache.logging.log4j.plugins.di;

import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Ordered;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO: add tests for meta namespace annotations
// TODO: add tests for more complex types with generics etc
class KeyTest {

    @Namespace("namespace")
    @Named("name")
    @Ordered(42)
    static class AnnotatedClass {
    }

    @Test
    void forClass() {
        final Key<AnnotatedClass> key = Key.forClass(AnnotatedClass.class);
        assertEquals("namespace", key.getNamespace());
        assertEquals("name", key.getName());
        assertEquals(42, key.getOrder());
        assertEquals(AnnotatedClass.class, key.getType());
        assertEquals(AnnotatedClass.class, key.getRawType());
    }

    interface AnnotatedMethod {
        @Namespace("foo")
        @Named("bar")
        @Ordered(10)
        String string();
    }

    @Test
    void forMethod() {
        final Key<String> key = assertDoesNotThrow(() -> Key.forMethod(AnnotatedMethod.class.getMethod("string")));
        assertEquals("foo", key.getNamespace());
        assertEquals("bar", key.getName());
        assertEquals(10, key.getOrder());
        assertEquals(String.class, key.getType());
        assertEquals(String.class, key.getRawType());
    }

    interface AnnotatedParameter {
        void method(@Namespace("foo") @Named String bar);
    }

    @Test
    void forParameter() {
        final Method method = assertDoesNotThrow(() -> AnnotatedParameter.class.getMethod("method", String.class));
        final Key<String> key = Key.forParameter(method.getParameters()[0]);
        assertEquals("foo", key.getNamespace());
        assertEquals("bar", key.getName());
        assertEquals(0, key.getOrder());
        assertEquals(String.class, key.getType());
        assertEquals(String.class, key.getRawType());
    }

    static class AnnotatedField {
        @Namespace("foo")
        @Named("bar")
        public String string;
    }

    @Test
    void forField() {
        final Field field = assertDoesNotThrow(() -> AnnotatedField.class.getField("string"));
        final Key<String> key = Key.forField(field);
        assertEquals("foo", key.getNamespace());
        assertEquals("bar", key.getName());
        assertEquals(0, key.getOrder());
        assertEquals(String.class, key.getType());
        assertEquals(String.class, key.getRawType());
    }
}
