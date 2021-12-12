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
package org.apache.logging.log4j.core.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.AccessMode;

import static org.apache.logging.log4j.core.net.SerializationHelper.extractClassName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SerializationHelperTest {
    @Test
    public void testClassNameExtractionForObject() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(42);
        assertEquals("java.lang.Integer", extractClassName(baos.toByteArray()),
                "Class name should be java.lang.Integer but is not");
    }

    @Test
    public void testClassNameExtractionForString() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject("test");
        assertEquals("java.lang.String", extractClassName(baos.toByteArray()),
                "Class name should be java.lang.String but is not");
    }

    @Test
    public void testClassNameExtractionForLongString() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(new String(new char[0xFFFF + 1]));
        assertEquals("java.lang.String", extractClassName(baos.toByteArray()),
                "Class name should be java.lang.String but is not");
    }

    @Test
    public void testClassNameExtractionForEnum() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(AccessMode.WRITE);
        assertEquals("java.nio.file.AccessMode", extractClassName(baos.toByteArray()),
                "Class name should be java.nio.file.AccessMode but is not");
    }

    @Test
    public void testClassNameExtractionForArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(new String[] {"foo", "bar"});
        assertNull(extractClassName(baos.toByteArray()),
                "Class name should null for an array but is not");
    }

    @Test
    public void testClassNameExtractionForNull() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(null);
        assertNull(extractClassName(baos.toByteArray()),
                "Class name should null for null but is not");
    }

    @Test
    public void testClassNameExtractionWithCorruptedSerializedForm() {
        assertNull(extractClassName(new byte[0]), "Class name should be null but is not");
    }
}
