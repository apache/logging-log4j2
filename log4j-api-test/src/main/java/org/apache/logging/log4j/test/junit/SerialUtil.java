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
package org.apache.logging.log4j.test.junit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import org.apache.logging.log4j.test.internal.annotation.SuppressFBWarnings;
import org.apache.logging.log4j.util.FilteredObjectInputStream;

/**
 * Utility class to facilitate serializing and deserializing objects.
 * <p>
 * The input streams created by this class are used by two kinds of tests:
 * </p>
 * <ul>
 * <li>If the {@code jdk.serialFilter} system property is set, they are plain {@link ObjectInputStream}s,
 * to test deserialization with the process-wide filter recommended by OpenJDK.</li>
 * <li>Otherwise, they are instances of the deprecated {@link FilteredObjectInputStream}, to test that the
 * serialized forms of Log4j classes only contain the classes it allows by default.
 * This is <strong>not</strong> a recommendation to use {@link FilteredObjectInputStream}.</li>
 * </ul>
 */
public class SerialUtil {

    private SerialUtil() {}

    /**
     * Serializes the specified object and returns the result as a byte array.
     * @param obj the object to serialize
     * @return the serialized object
     */
    public static byte[] serialize(final Serializable obj) {
        return serialize(new Serializable[] {obj});
    }

    /**
     * Serializes the specified object and returns the result as a byte array.
     * @param objs an array of objects to serialize
     * @return the serialized object
     */
    public static byte[] serialize(final Serializable... objs) {
        try {
            final ByteArrayOutputStream bas = new ByteArrayOutputStream(8192);
            final ObjectOutput oos = new ObjectOutputStream(bas);
            for (final Object obj : objs) {
                oos.writeObject(obj);
            }
            oos.flush();
            return bas.toByteArray();
        } catch (final Exception ex) {
            throw new IllegalStateException("Could not serialize", ex);
        }
    }

    /**
     * Deserialize an object from the specified byte array and returns the result.
     * @param data byte array representing the serialized object
     * @return the deserialized object
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static <T> T deserialize(final byte[] data) {
        return deserialize(data, Collections.emptySet());
    }

    /**
     * Deserialize an object from the specified byte array (see the class Javadoc).
     * @param data byte array representing the serialized object
     * @param allowedExtraClasses fully-qualified class names to add to the default allowlist of
     *     {@link FilteredObjectInputStream}
     * @return the deserialized object
     */
    @SuppressWarnings("unchecked")
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static <T> T deserialize(final byte[] data, final Collection<String> allowedExtraClasses) {
        try {
            final ObjectInputStream ois = getObjectInputStream(data, allowedExtraClasses);
            return (T) ois.readObject();
        } catch (final Exception ex) {
            throw new IllegalStateException("Could not deserialize", ex);
        }
    }

    /**
     * Creates an {@link ObjectInputStream} for the current kind of test (see the class Javadoc).
     * @param data data to deserialize,
     * @return an object input stream.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(final byte[] data) throws IOException {
        return getObjectInputStream(data, Collections.emptySet());
    }

    /**
     * Creates an {@link ObjectInputStream} for the current kind of test (see the class Javadoc).
     * The supplied extra classes are only used by {@link FilteredObjectInputStream}.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(
            final byte[] data, final Collection<String> allowedExtraClasses) throws IOException {
        final ByteArrayInputStream bas = new ByteArrayInputStream(data);
        return getObjectInputStream(bas, allowedExtraClasses);
    }

    /**
     * Creates an {@link ObjectInputStream} for the current kind of test (see the class Javadoc).
     * @param stream stream of data to deserialize,
     * @return an object input stream.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(final InputStream stream) throws IOException {
        return getObjectInputStream(stream, Collections.emptySet());
    }

    /**
     * Creates an {@link ObjectInputStream} for the current kind of test (see the class Javadoc).
     * The supplied extra classes are only used by {@link FilteredObjectInputStream}.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(
            final InputStream stream, final Collection<String> allowedExtraClasses) throws IOException {
        return System.getProperty("jdk.serialFilter") != null
                ? new ObjectInputStream(stream)
                : new FilteredObjectInputStream(stream, allowedExtraClasses);
    }
}
