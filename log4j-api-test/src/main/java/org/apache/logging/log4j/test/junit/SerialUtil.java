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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import org.apache.logging.log4j.test.internal.annotation.SuppressFBWarnings;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.FilteredObjectInputStream;

/**
 * Utility class to facilitate serializing and deserializing objects.
 */
public class SerialUtil {

    // On Java 9+ streams are filtered with `DefaultObjectInputFilter`, which must be accessed reflectively.
    private static final Method createFilter;
    private static final Method newDefaultObjectInputFilter;
    private static final Method setObjectInputFilter;

    static {
        Method createFilterMethod = null;
        Method newInstanceMethod = null;
        Method setFilterMethod = null;
        if (Constants.JAVA_MAJOR_VERSION != 8) {
            try {
                final Class<?> filterClass = Class.forName("java.io.ObjectInputFilter");
                createFilterMethod =
                        Class.forName("java.io.ObjectInputFilter$Config").getMethod("createFilter", String.class);
                newInstanceMethod = Class.forName("org.apache.logging.log4j.util.internal.DefaultObjectInputFilter")
                        .getMethod("newInstance", filterClass);
                setFilterMethod = ObjectInputStream.class.getMethod("setObjectInputFilter", filterClass);
            } catch (final ReflectiveOperationException e) {
                createFilterMethod = null;
                newInstanceMethod = null;
                // setFilterMethod is already null
            }
        }
        createFilter = createFilterMethod;
        newDefaultObjectInputFilter = newInstanceMethod;
        setObjectInputFilter = setFilterMethod;
    }

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
     * Creates an {@link ObjectInputStream} adapted to the current Java version.
     * @param data data to deserialize,
     * @return an object input stream.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(final byte[] data) throws IOException {
        return getObjectInputStream(data, Collections.emptySet());
    }

    /**
     * Creates an {@link ObjectInputStream} adapted to the current Java version, applying Log4j's
     * deserialization allow-list extended with the supplied extra classes.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(
            final byte[] data, final Collection<String> allowedExtraClasses) throws IOException {
        final ByteArrayInputStream bas = new ByteArrayInputStream(data);
        return getObjectInputStream(bas, allowedExtraClasses);
    }

    /**
     * Creates an {@link ObjectInputStream} adapted to the current Java version.
     * @param stream stream of data to deserialize,
     * @return an object input stream.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(final InputStream stream) throws IOException {
        return getObjectInputStream(stream, Collections.emptySet());
    }

    /**
     * Creates an {@link ObjectInputStream} adapted to the current Java version, applying Log4j's
     * deserialization allowlist extended with the supplied extra classes.
     */
    @SuppressFBWarnings("OBJECT_DESERIALIZATION")
    public static ObjectInputStream getObjectInputStream(
            final InputStream stream, final Collection<String> allowedExtraClasses) throws IOException {
        if (Constants.JAVA_MAJOR_VERSION == 8 || newDefaultObjectInputFilter == null) {
            return new FilteredObjectInputStream(stream, allowedExtraClasses);
        }
        final ObjectInputStream ois = new ObjectInputStream(stream);
        try {
            final Object extraClassesFilter = allowedExtraClasses.isEmpty()
                    ? null
                    : createFilter.invoke(null, String.join(";", allowedExtraClasses));
            setObjectInputFilter.invoke(ois, newDefaultObjectInputFilter.invoke(null, extraClassesFilter));
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to install the deserialization filter", e);
        }
        return ois;
    }
}
