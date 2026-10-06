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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.apache.logging.log4j.internal.annotation.SuppressFBWarnings;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.FilteredObjectInputStream;

/**
 * Provides methods to increase the safety of object serialization/deserialization.
 */
public final class SerializationUtil {

    private static final Method setObjectInputFilter;
    private static final Method getObjectInputFilter;

    static {
        Method setMethod = null;
        Method getMethod = null;
        for (final Method method : ObjectInputStream.class.getMethods()) {
            if (method.getName().equals("setObjectInputFilter")) {
                setMethod = method;
            } else if (method.getName().equals("getObjectInputFilter")) {
                getMethod = method;
            }
        }
        setObjectInputFilter = setMethod;
        getObjectInputFilter = getMethod;
    }

    public static final List<String> REQUIRED_JAVA_CLASSES = Arrays.asList(
            "java.math.BigDecimal",
            "java.math.BigInteger",
            // all primitives
            "boolean",
            "byte",
            "char",
            "double",
            "float",
            "int",
            "long",
            "short");

    public static final List<String> REQUIRED_JAVA_PACKAGES =
            Arrays.asList("java.lang.", "java.time.", "java.util.", "org.apache.logging.log4j.");

    /**
     * Writes the result of {@link #wrapObject} to the output stream.
     *
     * @param obj The object to write.
     * @param fallback Provides the value to write if {@code obj} cannot be serialized.
     * @param out The output stream.
     */
    public static void writeWrappedObject(
            final Object obj, final Supplier<Serializable> fallback, final ObjectOutputStream out) throws IOException {
        out.writeObject(wrapObject(obj, fallback));
    }

    /**
     * Serializes an object into a byte array.
     * <p>
     *     If the object is neither {@code null} nor {@link Serializable}, or its serialization fails, the value returned by
     *     {@code fallback} is serialized instead. Failed serializations are logged as warnings.
     * </p>
     *
     * @param obj The object to serialize.
     * @param fallback Provides the value to serialize if {@code obj} cannot be serialized.
     * @return The serialized form of {@code obj} or of the fallback value.
     */
    public static byte[] wrapObject(final Object obj, final Supplier<Serializable> fallback) throws IOException {
        if (obj == null || obj instanceof Serializable) {
            try {
                return toByteArray((Serializable) obj);
            } catch (final IOException | RuntimeException e) {
                StatusLogger.getLogger()
                        .warn("Unable to serialize an object of type {}, using a fallback value.", obj.getClass(), e);
            }
        }
        return toByteArray(fallback.get());
    }

    private static byte[] toByteArray(final Serializable obj) throws IOException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(bout)) {
            oos.writeObject(obj);
        }
        return bout.toByteArray();
    }

    /**
     * Reads an object written by {@link #writeWrappedObject}.
     *
     * @param in The input stream.
     * @return The object or {@code null}.
     * @see #unwrapObject
     */
    public static Object readWrappedObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        return unwrapObject(in, (byte[]) in.readObject());
    }

    /**
     * Deserializes an object serialized by {@link #wrapObject}.
     *
     * <p>The object is read from a nested stream, filtered in the same way as {@code in}.
     * If the class of the object is not available, {@code null} is returned and a warning is logged.
     * Other errors, including classes rejected by the filter, are thrown.</p>
     *
     * @param in The input stream that contained {@code data}.
     * @param data The serialized object.
     * @return The object or {@code null}.
     */
    @SuppressFBWarnings(
            value = "OBJECT_DESERIALIZATION",
            justification = "The nested stream is filtered in the same way as the outer stream.")
    @SuppressWarnings("deprecation")
    public static Object unwrapObject(final ObjectInputStream in, final byte[] data) throws IOException {
        final ByteArrayInputStream bin = new ByteArrayInputStream(data);
        final ObjectInputStream ois = in instanceof FilteredObjectInputStream
                ? new FilteredObjectInputStream(bin, ((FilteredObjectInputStream) in).getAllowedClasses())
                : new ObjectInputStream(bin);
        copyObjectInputFilter(in, ois);
        try {
            return ois.readObject();
        } catch (final IOException e) {
            // Includes classes rejected by the stream's filter
            throw e;
        } catch (final Exception | LinkageError e) {
            // The class is not available or not compatible
            StatusLogger.getLogger().warn("Ignoring {} during deserialization", e.getMessage());
            return null;
        } finally {
            ois.close();
        }
    }

    /**
     * Serializes each element of an array with {@link #wrapObject}.
     *
     * <p>Elements that cannot be serialized are replaced by their {@link String#valueOf(Object)} representation.</p>
     *
     * @param array An array or {@code null}.
     * @return The serialized elements or {@code null}.
     */
    public static byte[][] wrapObjects(final Object[] array) throws IOException {
        if (array == null) {
            return null;
        }
        final byte[][] wrapped = new byte[array.length][];
        for (int i = 0; i < array.length; i++) {
            final Object item = array[i];
            wrapped[i] = wrapObject(item, () -> String.valueOf(item));
        }
        return wrapped;
    }

    /**
     * Deserializes each element of an array serialized by {@link #wrapObjects}.
     *
     * @param in The input stream that contained {@code wrapped}.
     * @param wrapped The serialized elements or {@code null}.
     * @return The deserialized elements or {@code null}.
     * @see #unwrapObject
     */
    public static Object[] unwrapObjects(final ObjectInputStream in, final byte[][] wrapped) throws IOException {
        if (wrapped == null) {
            return null;
        }
        final Object[] array = new Object[wrapped.length];
        for (int i = 0; i < wrapped.length; i++) {
            array[i] = unwrapObject(in, wrapped[i]);
        }
        return array;
    }

    /**
     * Applies the {@code ObjectInputFilter} of {@code source} to {@code target} on Java 9 or later.
     */
    private static void copyObjectInputFilter(final ObjectInputStream source, final ObjectInputStream target)
            throws StreamCorruptedException {
        if (setObjectInputFilter == null) {
            return;
        }
        try {
            final Object filter = getObjectInputFilter.invoke(source);
            if (filter != null) {
                setObjectInputFilter.invoke(target, filter);
            }
        } catch (final IllegalAccessException | InvocationTargetException ex) {
            throw new StreamCorruptedException("Unable to set ObjectInputFilter on stream");
        }
    }

    /**
     * Gets the class name of an array component recursively.
     * <p>
     *     If {@code clazz} is not an array class its name is returned.
     * </p>
     * @param clazz the binary name of a class.
     */
    public static String stripArray(final Class<?> clazz) {
        Class<?> currentClazz = clazz;
        while (currentClazz.isArray()) {
            currentClazz = currentClazz.getComponentType();
        }
        return currentClazz.getName();
    }

    /**
     * Gets the class name of an array component recursively.
     * <p>
     *     If {@code name} is not the name of an array class it is returned unchanged.
     * </p>
     * @param name the name of a class.
     * @see Class#getName()
     */
    public static String stripArray(final String name) {
        final int offset = name.lastIndexOf('[') + 1;
        if (offset == 0) {
            return name;
        }
        // Reference types
        if (name.charAt(offset) == 'L') {
            return name.substring(offset + 1, name.length() - 1);
        }
        // Primitive classes
        switch (name.substring(offset)) {
            case "Z":
                return "boolean";
            case "B":
                return "byte";
            case "C":
                return "char";
            case "D":
                return "double";
            case "F":
                return "float";
            case "I":
                return "int";
            case "J":
                return "long";
            case "S":
                return "short";
            default:
                throw new IllegalArgumentException("Unsupported array class signature '" + name + "'");
        }
    }

    private SerializationUtil() {}
}
