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
package org.apache.logging.log4j.core.util.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.URI;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.apache.logging.log4j.util.FilteredObjectInputStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

/**
 * Tests that the nested stream of {@link WrappedObjects} applies the same restrictions as the outer stream.
 */
@Tag(Tags.SERIALIZATION)
class WrappedObjectsTest {

    static final class Payload implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    @Test
    void readWrappedObjectRoundTrip() throws Exception {
        final byte[] data = writeWrapped(new Payload());

        assertThat(WrappedObjects.readWrappedObject(new ObjectInputStream(new ByteArrayInputStream(data))))
                .isInstanceOf(Payload.class);
    }

    @Test
    @SuppressWarnings("deprecation")
    void readWrappedObjectUsesAllowlistOfOuterFilteredStream() throws Exception {
        // `Payload` is in `org.apache.logging.log4j.`, so it is only rejected if the allowlist is used
        final byte[] data = writeWrapped(new Payload());

        final ObjectInputStream in = new FilteredObjectInputStream(new ByteArrayInputStream(data));
        assertThat(WrappedObjects.readWrappedObject(in)).isInstanceOf(Payload.class);

        final ObjectInputStream restricted = new FilteredObjectInputStream(
                new ByteArrayInputStream(writeWrapped(URI.create("https://logging.apache.org/"))));
        assertThatThrownBy(() -> WrappedObjects.readWrappedObject(restricted))
                .isInstanceOf(ObjectStreamException.class);
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_9)
    void readWrappedObjectUsesObjectInputFilterOfOuterStream() throws Exception {
        final byte[] data = writeWrapped(new Payload());

        final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
        setObjectInputFilter(in, "!" + Payload.class.getName());
        assertThatThrownBy(() -> WrappedObjects.readWrappedObject(in)).isInstanceOf(InvalidClassException.class);
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_9)
    @SuppressWarnings("deprecation")
    void readWrappedObjectUsesObjectInputFilterOfOuterFilteredStream() throws Exception {
        // `Payload` is in `org.apache.logging.log4j.`, so only the filter can reject it
        final byte[] data = writeWrapped(new Payload());

        final ObjectInputStream in = new FilteredObjectInputStream(new ByteArrayInputStream(data));
        setObjectInputFilter(in, "!" + Payload.class.getName());
        assertThatThrownBy(() -> WrappedObjects.readWrappedObject(in)).isInstanceOf(InvalidClassException.class);
    }

    private static byte[] writeWrapped(final Serializable obj) throws IOException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (final ObjectOutputStream out = new ObjectOutputStream(bout)) {
            WrappedObjects.writeWrappedObject(obj, () -> null, out);
        }
        return bout.toByteArray();
    }

    /**
     * Java 9 API accessed through reflection, since this module targets Java 8.
     */
    private static void setObjectInputFilter(final ObjectInputStream in, final String pattern) throws Exception {
        final Class<?> filterClass = Class.forName("java.io.ObjectInputFilter");
        final Object filter = Class.forName("java.io.ObjectInputFilter$Config")
                .getMethod("createFilter", String.class)
                .invoke(null, pattern);
        ObjectInputStream.class.getMethod("setObjectInputFilter", filterClass).invoke(in, filter);
    }
}
