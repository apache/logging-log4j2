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
 * Objects are deserialized with {@link FilteredObjectInputStream}, to check that the serialized forms of Log4j
 * classes only contain the classes allowed by default.
 * This is <strong>not</strong> a recommendation to use {@link FilteredObjectInputStream}:
 * applications should restrict deserialization with an {@code ObjectInputFilter},
 * for example using the {@code jdk.serialFilter} system property.
 * </p>
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
     * Deserialize an object from the specified byte array using a stream that applies Log4j's
     * deserialization allow-list, extended with the supplied extra classes.
     * @param data byte array representing the serialized object
     * @param allowedExtraClasses fully-qualified class names to add to the default allow-list
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
     * Creates a {@link FilteredObjectInputStream} that applies Log4j's deserialization allowlist.
     * @param data data to deserialize,
     * @return an object input stream.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(final byte[] data) throws IOException {
        return getObjectInputStream(data, Collections.emptySet());
    }

    /**
     * Creates a {@link FilteredObjectInputStream} that applies Log4j's
     * deserialization allowlist, extended with the supplied extra classes.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(
            final byte[] data, final Collection<String> allowedExtraClasses) throws IOException {
        final ByteArrayInputStream bas = new ByteArrayInputStream(data);
        return getObjectInputStream(bas, allowedExtraClasses);
    }

    /**
     * Creates a {@link FilteredObjectInputStream} that applies Log4j's deserialization allowlist.
     * @param stream stream of data to deserialize,
     * @return an object input stream.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(final InputStream stream) throws IOException {
        return getObjectInputStream(stream, Collections.emptySet());
    }

    /**
     * Creates a {@link FilteredObjectInputStream} that applies Log4j's
     * deserialization allowlist, extended with the supplied extra classes.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(
            final InputStream stream, final Collection<String> allowedExtraClasses) throws IOException {
        return new FilteredObjectInputStream(stream, allowedExtraClasses);
    }
}
