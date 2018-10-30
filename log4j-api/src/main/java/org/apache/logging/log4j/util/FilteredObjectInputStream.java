/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Extended ObjectInputStream that only allows certain classes to be deserialized.
 *
 * @since 2.8.2
 */
public class FilteredObjectInputStream extends ObjectInputStream {

    private static final List<String> REQUIRED_JAVA_CLASSES = Arrays.asList(
            "java.math.BigDecimal",
            "java.math.BigInteger",
            // for Message delegate
            "java.rmi.MarshalledObject",
            "[B"
    );

    private static final List<String> REQUIRED_JAVA_PACKAGES = Arrays.asList(
            "java.lang.",
            "java.time",
            "java.util.",
            "org.apache.logging.log4j.",
            "[Lorg.apache.logging.log4j."
    );

    private final Collection<String> allowedClasses;

    public FilteredObjectInputStream() throws IOException, SecurityException {
        super();
        this.allowedClasses = new HashSet<>();
    }

    public FilteredObjectInputStream(final InputStream in) throws IOException {
        super(in);
        this.allowedClasses = new HashSet<>();
    }

    public FilteredObjectInputStream(final Collection<String> allowedClasses) throws IOException, SecurityException {
        super();
        this.allowedClasses = allowedClasses;
    }

    public FilteredObjectInputStream(final InputStream in, final Collection<String> allowedClasses) throws IOException {
        super(in);
        this.allowedClasses = allowedClasses;
    }

    public Collection<String> getAllowedClasses() {
        return allowedClasses;
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        final String name = desc.getName();
        if (!(isAllowedByDefault(name) || allowedClasses.contains(name))) {
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
