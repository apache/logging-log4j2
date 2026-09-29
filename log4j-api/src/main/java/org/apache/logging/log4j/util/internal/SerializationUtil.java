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
import org.apache.logging.log4j.internal.annotation.SuppressFBWarnings;
import org.apache.logging.log4j.status.StatusLogger;

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
            // for Message delegate
            "java.rmi.MarshalledObject",
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

    public static void writeWrappedObject(final Serializable obj, final ObjectOutputStream out) throws IOException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(bout)) {
            oos.writeObject(obj);
            oos.flush();
            out.writeObject(bout.toByteArray());
        }
    }

    @SuppressFBWarnings(
            value = "OBJECT_DESERIALIZATION",
            justification = "The nested stream is filtered in the same way as the outer stream.")
    @SuppressWarnings("deprecation")
    public static Object readWrappedObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        final byte[] data = (byte[]) in.readObject();
        final ByteArrayInputStream bin = new ByteArrayInputStream(data);
        final ObjectInputStream ois = in instanceof org.apache.logging.log4j.util.FilteredObjectInputStream
                ? new org.apache.logging.log4j.util.FilteredObjectInputStream(
                        bin, ((org.apache.logging.log4j.util.FilteredObjectInputStream) in).getAllowedClasses())
                : new ObjectInputStream(bin);
        copyObjectInputFilter(in, ois);
        try {
            return ois.readObject();
        } catch (final Exception | LinkageError e) {
            StatusLogger.getLogger().warn("Ignoring {} during deserialization", e.getMessage());
            return null;
        } finally {
            ois.close();
        }
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
