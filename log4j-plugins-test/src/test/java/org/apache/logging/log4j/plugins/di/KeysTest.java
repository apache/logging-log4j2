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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.function.Function;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Node;
import org.junit.jupiter.api.Test;

class KeysTest {
    @Test
    void annotatedTypeUsage() {
        Key<Function<String, String>> substitutorKey = new @Named("StringSubstitutor") Key<>() {};
        var actualKey = new Key<Function<String, String>>() {}.withName("StringSubstitutor")
                .withQualifierType(Named.class);
        assertEquals(substitutorKey, actualKey);
    }

    @Test
    void configurableNamespace() {
        final Key<String> key = new @Configurable Key<>() {};
        assertEquals(Node.CORE_NAMESPACE, key.getNamespace());
    }

    static class ConfigurableField {
        @Configurable
        String field;
    }

    @Test
    void fieldWithMetaNamespace() {
        final Field field = assertDoesNotThrow(() -> ConfigurableField.class.getDeclaredField("field"));
        final Key<String> key = Key.forField(field);
        assertEquals(Node.CORE_NAMESPACE, key.getNamespace());
    }

    static class ConfigurableParameter {
        ConfigurableParameter(@Configurable String parameter) {}
    }

    @Test
    void parameterWithMetaNamespace() {
        final Constructor<ConfigurableParameter> constructor =
                assertDoesNotThrow(() -> ConfigurableParameter.class.getDeclaredConstructor(String.class));
        final Parameter parameter = constructor.getParameters()[0];
        final Key<String> key = Key.forParameter(parameter);
        assertEquals(Node.CORE_NAMESPACE, key.getNamespace());
    }
}
