/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

package org.apache.log4j.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.io.FileUtils;


/**
 * Utiities for serialization tests.
 */
public class SerializationTestHelper {
    /**
     * Private constructor.
     */
    private SerializationTestHelper() {
    }

    /**
     * Creates a clone by serializing object and
     * deserializing byte stream.
     *
     * @param obj object to serialize and deserialize.
     * @return clone
     * @throws IOException            on IO error.
     * @throws ClassNotFoundException if class not found.
     */
    public static Object serializeClone(final Object obj)
        throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        try (final ObjectOutputStream objOut = new ObjectOutputStream(memOut)) {
            objOut.writeObject(obj);
        }

        final ByteArrayInputStream src = new ByteArrayInputStream(memOut.toByteArray());
        final ObjectInputStream objIs = new ObjectInputStream(src);

        return objIs.readObject();
    }

    /**
     * Deserializes a specified file.
     *
     * @param witness serialization file, may not be null.
     * @return deserialized object.
     * @throws Exception thrown on IO or deserialization exception.
     */
    public static Object deserializeStream(final String witness) throws Exception {
        try (final ObjectInputStream objIs = new ObjectInputStream(new FileInputStream(witness))) {
            return objIs.readObject();
        }
    }

    /**
     * Checks the serialization of an object against an file
     * containing the expected serialization.
     *
     * @param witness    name of file containing expected serialization.
     * @param obj        object to be serialized.
     * @param skip       positions in serialized stream that should not be compared.
     * @param endCompare position to stop comparison.
     * @throws Exception thrown on IO or serialization exception.
     */
    public static void assertSerializationEquals(
        final String witness, final Object obj, final int[] skip,
        final int endCompare) throws Exception {
        final ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        try (final ObjectOutputStream objOut = new ObjectOutputStream(memOut)) {
            objOut.writeObject(obj);
        }

        assertStreamEquals(witness, memOut.toByteArray(), skip, endCompare);
    }

    /**
     * Asserts the serialized form of an object.
     *
     * @param witness    file name of expected serialization.
     * @param actual     byte array of actual serialization.
     * @param skip       positions to skip comparison.
     * @param endCompare position to stop comparison.
     * @throws IOException thrown on IO or serialization exception.
     */
    public static void assertStreamEquals(
        final String witness, final byte[] actual, final int[] skip,
        final int endCompare) throws IOException {
        final File witnessFile = new File(witness);

        if (witnessFile.exists()) {
            int skipIndex = 0;
            final byte[] expected = FileUtils.readFileToByteArray(witnessFile);
            final int bytesRead = expected.length;

            if (bytesRead < endCompare) {
                assertEquals(bytesRead, actual.length);
            }

            int endScan = actual.length;

            if (endScan > endCompare) {
                endScan = endCompare;
            }

            for (int i = 0; i < endScan; i++) {
                if ((skipIndex < skip.length) && (skip[skipIndex] == i)) {
                    skipIndex++;
                } else {
                    if (expected[i] != actual[i]) {
                        assertEquals(
                            "Difference at offset " + i, expected[i], actual[i]);
                    }
                }
            }
        } else {
            //
            //  if the file doesn't exist then
            //      assume that we are setting up and need to write it
            FileUtils.writeByteArrayToFile(witnessFile, actual);
            fail("Writing witness file " + witness);
        }
    }
}

