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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.FilteredObjectInputStream;

/**
 * Provides methods to increase the safety of object serialization/deserialization.
 */
public final class SerializationUtil {

    private static final String DEFAULT_FILTER_CLASS =
            "org.apache.logging.log4j.util.internal.DefaultObjectInputFilter";
    private static final Method setObjectInputFilter;
    private static final Method getObjectInputFilter;
    private static final Method newObjectInputFilter;

    static {
        Method[] methods = ObjectInputStream.class.getMethods();
        Method setMethod = null;
        Method getMethod = null;
        for (final Method method : methods) {
            if (method.getName().equals("setObjectInputFilter")) {
                setMethod = method;
            } else if (method.getName().equals("getObjectInputFilter")) {
                getMethod = method;
            }
        }
        Method newMethod = null;
        try {
            if (setMethod != null) {
                final Class<?> clazz = Class.forName(DEFAULT_FILTER_CLASS);
                methods = clazz.getMethods();
                for (final Method method : methods) {
                    if (method.getName().equals("newInstance") && Modifier.isStatic(method.getModifiers())) {
                        newMethod = method;
                        break;
                    }
                }
            }
        } catch (final ClassNotFoundException ex) {
            // Ignore the exception
        }
        newObjectInputFilter = newMethod;
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
            justification =
                    "Object deserialization uses either Java 9 native filter or our custom filter to limit the kinds of classes deserialized.")
    public static Object readWrappedObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        assertFiltered(in);
        final byte[] data = (byte[]) in.readObject();
        final ByteArrayInputStream bin = new ByteArrayInputStream(data);
        final ObjectInputStream ois;
        if (in instanceof FilteredObjectInputStream) {
            ois = new FilteredObjectInputStream(bin, ((FilteredObjectInputStream) in).getAllowedClasses());
        } else {
            try {
                final Object obj = getObjectInputFilter.invoke(in);
                final Object filter = newObjectInputFilter.invoke(null, obj);
                ois = new ObjectInputStream(bin);
                setObjectInputFilter.invoke(ois, filter);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new StreamCorruptedException("Unable to set ObjectInputFilter on stream");
            }
        }
        try {
            return ois.readObject();
        } catch (final Exception | LinkageError e) {
            StatusLogger.getLogger().warn("Ignoring {} during deserialization", e.getMessage());
            return null;
        } finally {
            ois.close();
        }
    }

    public static void assertFiltered(final java.io.ObjectInputStream stream) {
        if (!(stream instanceof FilteredObjectInputStream) && setObjectInputFilter == null) {
            throw new IllegalArgumentException(
                    "readObject requires a FilteredObjectInputStream or an ObjectInputStream that accepts an ObjectInputFilter");
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
