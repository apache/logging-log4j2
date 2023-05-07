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
package org.apache.logging.log4j.plugins.di;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.plugins.name.NameProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(Named.class, key.getQualifierType());
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
        assertEquals(Named.class, key.getQualifierType());
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
        assertEquals(Named.class, key.getQualifierType());
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
        assertEquals(Named.class, key.getQualifierType());
        assertEquals(0, key.getOrder());
        assertEquals(String.class, key.getType());
        assertEquals(String.class, key.getRawType());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @QualifierType
    @NameProvider(CustomQualifierNameProvider.class)
    @interface CustomQualifier {
        String value();
    }

    static class CustomQualifierNameProvider implements AnnotatedElementNameProvider<CustomQualifier> {
        @Override
        public Optional<String> getSpecifiedName(final CustomQualifier annotation) {
            return Optional.of(annotation.value());
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Namespace("logical")
    @interface LogicalNamespace {}

    @LogicalNamespace
    @CustomQualifier("class")
    static class LogicallyAnnotatedClass {}

    @Test
    void classWithCustomQualifierAndNamespace() {
        final Key<LogicallyAnnotatedClass> key = Key.forClass(LogicallyAnnotatedClass.class);
        assertEquals("logical", key.getNamespace());
        assertEquals("class", key.getName());
        assertEquals(CustomQualifier.class, key.getQualifierType());
        assertEquals(0, key.getOrder());
        assertEquals(LogicallyAnnotatedClass.class, key.getType());
        assertEquals(LogicallyAnnotatedClass.class, key.getRawType());
    }

    interface LogicallyAnnotatedMethod {
        @LogicalNamespace
        @CustomQualifier("method")
        String string();
    }

    @Test
    void methodWithCustomQualifierAndNamespace() {
        final Method method = assertDoesNotThrow(() -> LogicallyAnnotatedMethod.class.getDeclaredMethod("string"));
        final Key<String> key = Key.forMethod(method);
        assertEquals("logical", key.getNamespace());
        assertEquals("method", key.getName());
        assertEquals(CustomQualifier.class, key.getQualifierType());
        assertEquals(0, key.getOrder());
        assertEquals(String.class, key.getType());
        assertEquals(String.class, key.getRawType());
    }

    interface LogicallyAnnotatedParameter {
        void inject(@LogicalNamespace @CustomQualifier("parameter") String argument);
    }

    @Test
    void parameterWithCustomQualifierAndNamespace() {
        final Parameter parameter = assertDoesNotThrow(() ->
                LogicallyAnnotatedParameter.class.getDeclaredMethod("inject", String.class).getParameters()[0]);
        final Key<String> key = Key.forParameter(parameter);
        assertEquals("logical", key.getNamespace());
        assertEquals("parameter", key.getName());
        assertEquals(CustomQualifier.class, key.getQualifierType());
        assertEquals(0, key.getOrder());
        assertEquals(String.class, key.getType());
        assertEquals(String.class, key.getRawType());
    }
}
