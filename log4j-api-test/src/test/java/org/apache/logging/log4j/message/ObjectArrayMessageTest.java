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
package org.apache.logging.log4j.message;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import org.apache.logging.log4j.test.junit.SerialUtil;
import org.junit.jupiter.api.Test;

/**
 * @since 2.4
 */
class ObjectArrayMessageTest {

    private static final Object[] ARRAY = {"A", "B", "C"};
    private static final ObjectArrayMessage OBJECT_ARRAY_MESSAGE = new ObjectArrayMessage(ARRAY);

    @Test
    void testGetParameters() {
        assertArrayEquals(ARRAY, OBJECT_ARRAY_MESSAGE.getParameters());
    }

    @Test
    void testGetThrowable() {
        assertNull(OBJECT_ARRAY_MESSAGE.getThrowable());
    }

    /**
     * Round-trips through a filtered stream (see {@link SerialUtil#getObjectInputStream})
     * to verify that {@code readObject}'s new {@code SerializationUtil.assertFiltered}
     * check accepts streams that carry a filter.
     */
    @Test
    void testSerializableRoundTripThroughFilteredStream() {
        final ObjectArrayMessage original = new ObjectArrayMessage("A", "B", "C");
        final ObjectArrayMessage restored = SerialUtil.deserialize(SerialUtil.serialize(original));
        assertArrayEquals(original.getParameters(), restored.getParameters());
    }

    @Test
    void testNonSerializableElementIsReplacedByItsStringForm() {
        final ObjectArrayMessage original = new ObjectArrayMessage("A", new NonSerializable(), "C");
        final ObjectArrayMessage restored = SerialUtil.deserialize(SerialUtil.serialize(original));
        assertArrayEquals(new Object[] {"A", "non-serializable", "C"}, restored.getParameters());
    }

    @Test
    void testDeserializationResizesBeyondInitialAllocation() {
        // One element more than the bounded initial allocation of the deserialized array
        final Object[] array = new Object[(1 << 8) + 1];
        for (int i = 0; i < array.length; i++) {
            array[i] = String.format("%08x", i);
        }
        final ObjectArrayMessage restored = SerialUtil.deserialize(SerialUtil.serialize(new ObjectArrayMessage(array)));
        assertArrayEquals(array, restored.getParameters());
    }

    @Test
    void testDeserializationDoesNotPreallocateDeclaredLength() throws Exception {
        // A forged stream declaring a huge length must fail on the missing data, not allocate for it.
        final byte[] forged =
                patchLength(SerialUtil.serialize(new ObjectArrayMessage("A", "B", "C")), 3, Integer.MAX_VALUE);
        final ObjectInputStream ois = SerialUtil.getObjectInputStream(forged);
        assertThrows(IOException.class, ois::readObject);
    }

    @Test
    void testDeserializationRejectsNegativeLength() throws Exception {
        final byte[] forged = patchLength(SerialUtil.serialize(new ObjectArrayMessage("A", "B", "C")), 3, -1);
        final ObjectInputStream ois = SerialUtil.getObjectInputStream(forged);
        assertThrows(InvalidObjectException.class, ois::readObject);
    }

    /**
     * Returns a copy of the serialized form with the declared array length replaced.
     * <p>
     *     The length follows the default field data as a block-data record ({@code 0x77}, length {@code 0x04}),
     *     so it can be located by its original value.
     * </p>
     */
    private static byte[] patchLength(final byte[] binary, final int length, final int newLength) {
        final ByteBuffer buffer = ByteBuffer.wrap(binary.clone());
        for (int i = 0; i + 6 <= binary.length; i++) {
            if (buffer.get(i) == 0x77 && buffer.get(i + 1) == 0x04 && buffer.getInt(i + 2) == length) {
                buffer.putInt(i + 2, newLength);
                return buffer.array();
            }
        }
        throw new AssertionError("Unable to locate the length field in the serialized form");
    }

    private static final class NonSerializable {
        @Override
        public String toString() {
            return "non-serializable";
        }
    }
}
