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
package org.apache.logging.log4j.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class SourceTest {

    @Test
    void testEqualityByLocation() {
        assertEquals(new Source(new File("foo")), new Source(new File("foo")));
        assertEquals(new Source(Paths.get("foo")), new Source(Paths.get("foo")));
        assertEquals(
                new Source(URI.create("http://www.apache.org/index.html")),
                new Source(URI.create("http://www.apache.org/index.html")));
        assertNotEquals(new Source(new File("foo")), new Source(new File("bar")));
    }

    @Test
    void testToStringUsesLocation() {
        final Source source = new Source(URI.create("http://www.apache.org/"));
        assertEquals("http://www.apache.org/", source.toString());
    }

    @Test
    void testHttpSourceHasNoFile() {
        final Source source = new Source(URI.create("http://www.apache.org/"));
        assertNull(source.getFile());
        assertEquals("http://www.apache.org/", source.getLocation());
    }

    @Test
    void testSerializableRoundTrip() throws Exception {
        final Source original = new Source(Paths.get("foo"));
        final Source restored = serializeRoundTrip(original);

        assertNotSame(original, restored);
        assertEquals(original, restored);
    }

    private static <T> T serializeRoundTrip(final T object) throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            @SuppressWarnings("unchecked")
            final T restored = (T) ois.readObject();
            return restored;
        }
    }
}
