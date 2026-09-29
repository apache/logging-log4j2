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
package org.apache.logging.log4j.util.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.stream.Stream;
import org.apache.logging.log4j.message.ObjectMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SerializationUtilTest {

    static Stream<Arguments> arrays() {
        return Stream.of(
                Arguments.of(boolean[].class, boolean.class),
                Arguments.of(char[].class, char.class),
                Arguments.of(byte[].class, byte.class),
                Arguments.of(short[].class, short.class),
                Arguments.of(int[].class, int.class),
                Arguments.of(long[].class, long.class),
                Arguments.of(float[].class, float.class),
                Arguments.of(double[].class, double.class),
                Arguments.of(String.class, String.class),
                Arguments.of(String[].class, String.class),
                Arguments.of(String[][].class, String.class));
    }

    @ParameterizedTest
    @MethodSource("arrays")
    void stripArrayClass(final Class<?> arrayClass, final Class<?> componentClazz) {
        assertThat(SerializationUtil.stripArray(arrayClass)).isEqualTo(componentClazz.getName());
    }

    @ParameterizedTest
    @MethodSource("arrays")
    void stripArrayString(final Class<?> arrayClass, final Class<?> componentClazz) {
        assertThat(SerializationUtil.stripArray(arrayClass.getName())).isEqualTo(componentClazz.getName());
    }

    static final class Payload implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_9)
    void readWrappedObjectUsesObjectInputFilterOfOuterStream() throws Exception {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (final ObjectOutputStream out = new ObjectOutputStream(bout)) {
            out.writeObject(new ObjectMessage(new Payload()));
        }
        final byte[] data = bout.toByteArray();

        final ObjectInputStream unfiltered = new ObjectInputStream(new ByteArrayInputStream(data));
        assertThat(((ObjectMessage) unfiltered.readObject()).getParameter()).isInstanceOf(Payload.class);

        // Java 9 API accessed through reflection, since this module targets Java 8
        final Class<?> filterClass = Class.forName("java.io.ObjectInputFilter");
        final Object filter = Class.forName("java.io.ObjectInputFilter$Config")
                .getMethod("createFilter", String.class)
                .invoke(null, "!" + Payload.class.getName());
        final ObjectInputStream filtered = new ObjectInputStream(new ByteArrayInputStream(data));
        ObjectInputStream.class.getMethod("setObjectInputFilter", filterClass).invoke(filtered, filter);
        assertThat(((ObjectMessage) filtered.readObject()).getParameter()).isNull();
    }
}
