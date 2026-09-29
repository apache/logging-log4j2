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
package org.apache.logging.log4j.util;

import static org.apache.logging.log4j.util.internal.SerializationUtil.REQUIRED_JAVA_CLASSES;
import static org.apache.logging.log4j.util.internal.SerializationUtil.REQUIRED_JAVA_PACKAGES;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Collection;
import java.util.Collections;
import org.apache.logging.log4j.util.internal.SerializationUtil;

/**
 * An {@link ObjectInputStream} that only deserializes a fixed list of classes and caller-specified classes.
 *
 * <p>The classes allowed by default are:</p>
 * <ul>
 * <li>primitive types,</li>
 * <li>{@code java.math.BigDecimal} and {@code java.math.BigInteger},</li>
 * <li>{@code java.rmi.MarshalledObject},</li>
 * <li>all classes whose name starts with {@code java.lang.}, {@code java.time.}, {@code java.util.}
 * or {@code org.apache.logging.log4j.}.</li>
 * </ul>
 * <p>Arrays of any type in the above list are allowed.</p>
 *
 * <p><strong>Warning:</strong> deserialization of untrusted data is inherently dangerous and should be avoided
 * (see the {@link ObjectInputStream} Javadoc).
 * This class is <strong>not</strong> a security boundary:
 * using it does not make the deserialization of untrusted data safe.</p>
 *
 * <p>Like every class in this package, this class is internal:
 * the use of any classes in this package is not supported.
 * There are no guarantees for binary or logical compatibility in this package.</p>
 *
 * <p>Log4j itself does not deserialize data using this class.
 * If an application does, that is the application's responsibility:
 * the Log4j project does not consider weaknesses of this class to be vulnerabilities.
 * See the <a href="https://logging.apache.org/security/faq.html#deserialization-filtered-object-input-stream">Security FAQ</a>
 * and the <a href="https://logging.apache.org/security.html#threat-common">Log4j threat model</a>.</p>
 *
 * <p>This class was created as a substitute for {@code ObjectInputFilter} on Java 8 or earlier.
 * It should <strong>not</strong> be used in new code.
 * Applications that deserialize data should apply their own {@code ObjectInputFilter} to the stream,
 * or a process-wide filter using the {@code jdk.serialFilter} system property,
 * which is also available on Java 8 since update 121.
 * On Java 9 or later, Log4j applies the {@code ObjectInputFilter} of the stream
 * to the nested streams it creates while deserializing Log4j classes.</p>
 *
 * @since 2.11.0
 * @deprecated Since 2.27.0, without replacement.
 */
@Deprecated
public class FilteredObjectInputStream extends ObjectInputStream {

    private final Collection<String> allowedExtraClasses;

    public FilteredObjectInputStream() throws IOException, SecurityException {
        this.allowedExtraClasses = Collections.emptySet();
    }

    public FilteredObjectInputStream(final InputStream inputStream) throws IOException {
        super(inputStream);
        this.allowedExtraClasses = Collections.emptySet();
    }

    public FilteredObjectInputStream(final Collection<String> allowedExtraClasses)
            throws IOException, SecurityException {
        this.allowedExtraClasses = allowedExtraClasses;
    }

    public FilteredObjectInputStream(final InputStream inputStream, final Collection<String> allowedExtraClasses)
            throws IOException {
        super(inputStream);
        this.allowedExtraClasses = allowedExtraClasses;
    }

    public Collection<String> getAllowedClasses() {
        return allowedExtraClasses;
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        final String name = SerializationUtil.stripArray(desc.getName());
        if (!(isAllowedByDefault(name) || allowedExtraClasses.contains(name))) {
            throw new InvalidObjectException("Class is not allowed for deserialization: " + name);
        }
        return super.resolveClass(desc);
    }

    /**
     * Unconditionally rejects dynamic proxy classes.
     *
     * <p>No supported Log4j serialized form contains a dynamic proxy.</p>
     */
    @Override
    protected Class<?> resolveProxyClass(final String[] interfaces) throws IOException, ClassNotFoundException {
        throw new InvalidObjectException("Proxy classes are not allowed for deserialization");
    }

    private static boolean isAllowedByDefault(final String name) {
        return isRequiredPackage(name) || REQUIRED_JAVA_CLASSES.contains(name);
    }

    private static boolean isRequiredPackage(final String name) {
        for (final String packageName : REQUIRED_JAVA_PACKAGES) {
            if (name.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }
}
