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
package org.apache.logging.log4j.core.util.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import org.apache.logging.log4j.core.internal.annotation.SuppressFBWarnings;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.FilteredObjectInputStream;

/**
 * Wraps serialized objects in byte arrays, so that unserializable or unavailable classes don't break
 * the serialization of {@link org.apache.logging.log4j.core.impl.Log4jLogEvent}.
 * <p>
 *     Subset of {@code org.apache.logging.log4j.util.internal.SerializationUtil}, which is not exported by
 *     {@code log4j-api} on this branch.
 * </p>
 */
public final class WrappedObjects {

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

    /**
     * Writes an object as a byte array, falling back to {@code fallback} if it cannot be serialized.
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
     * Reads an object written by {@link #writeWrappedObject}.
     *
     * @param in The input stream.
     * @return The object or {@code null}.
     */
    public static Object readWrappedObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        return unwrapObject(in, (byte[]) in.readObject());
    }

    private static byte[] wrapObject(final Object obj, final Supplier<Serializable> fallback) throws IOException {
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

    @SuppressFBWarnings(
            value = "OBJECT_DESERIALIZATION",
            justification = "The nested stream is filtered in the same way as the outer stream.")
    @SuppressWarnings("deprecation")
    private static Object unwrapObject(final ObjectInputStream in, final byte[] data) throws IOException {
        final ByteArrayInputStream bin = new ByteArrayInputStream(data);
        final ObjectInputStream ois = in instanceof FilteredObjectInputStream
                ? new FilteredObjectInputStream(bin, ((FilteredObjectInputStream) in).getAllowedClasses())
                : new ObjectInputStream(bin);
        copyObjectInputFilter(in, ois);
        try {
            return ois.readObject();
        } catch (final IOException e) {
            throw e;
        } catch (final Exception | LinkageError e) {
            StatusLogger.getLogger().warn("Ignoring {} during deserialization", e.getMessage());
            return null;
        } finally {
            ois.close();
        }
    }

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

    private WrappedObjects() {}
}
