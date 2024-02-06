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
 * Extends {@link ObjectInputStream} to only allow some built-in Log4j classes and caller-specified classes to be
 * deserialized.
 *
 * @since 2.8.2
 */
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
