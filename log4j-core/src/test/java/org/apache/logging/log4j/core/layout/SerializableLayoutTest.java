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
package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.test.layout.SerializableLayout;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Verifies an implementation of AbstractStringLayout will serialize and deserialize properly to address LOG4J2-1099.
 */
public class SerializableLayoutTest {

    public static final String HEADER = "Test Header" + Constants.LINE_SEPARATOR;
    public static final String FOOTER = "Test Footer" + Constants.LINE_SEPARATOR;
    public static final String MESSAGE_PREFIX = "TEST PREFIX: ";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testPluginObjectSerialization() throws Exception {
        final SerializableLayout originalLayout = new SerializableLayout(MESSAGE_PREFIX, Charset.defaultCharset(),
                HEADER.getBytes(), FOOTER.getBytes());
        final File serializedFile = temporaryFolder.newFile();
        try (final FileOutputStream fileOutputStream = new FileOutputStream(serializedFile)) {
            final ObjectOutputStream out = new ObjectOutputStream(fileOutputStream);
            out.writeObject(originalLayout);
        }
        SerializableLayout serializedLayout;
        try (final FileInputStream fileInputStream = new FileInputStream(serializedFile)) {
            final ObjectInputStream in = new ObjectInputStream(fileInputStream);
            serializedLayout = (SerializableLayout) in.readObject();
        }
        assertEquals(originalLayout.getMessagePrefix(), serializedLayout.getMessagePrefix());
        assertEquals(originalLayout.getCharset(), serializedLayout.getCharset());
        assertArrayEquals(originalLayout.getHeader(), serializedLayout.getHeader());
        assertArrayEquals(originalLayout.getFooter(), serializedLayout.getFooter());
    }
}
