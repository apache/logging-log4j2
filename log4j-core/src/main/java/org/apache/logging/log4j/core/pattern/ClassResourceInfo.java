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
package org.apache.logging.log4j.core.pattern;

import java.net.URL;
import java.security.CodeSource;

/**
 * Resource information (i.e., the enclosing JAR file and its version) of a class.
 */
final class ClassResourceInfo {

    static final ClassResourceInfo UNKNOWN = new ClassResourceInfo();

    private final String text;

    final Class<?> clazz;

    /**
     * Constructs an instance modelling an unknown class resource.
     */
    private ClassResourceInfo() {
        this.text = "~[?:?]";
        this.clazz = null;
    }

    /**
     * @param clazz the class
     * @param exact {@code true}, if the class was obtained via reflection; {@code false}, otherwise
     */
    ClassResourceInfo(final Class<?> clazz, final boolean exact) {
        this.clazz = clazz;
        this.text = getText(clazz, exact);
    }

    private static String getText(final Class<?> clazz, final boolean exact) {
        final String exactnessPrefix = exact ? "" : "~";
        final String location = getLocation(clazz);
        final String version = getVersion(clazz);
        return String.format("%s[%s:%s]", exactnessPrefix, location, version);
    }

    private static String getLocation(final Class<?> clazz) {
        try {
            final CodeSource source = clazz.getProtectionDomain().getCodeSource();
            if (source != null) {
                final URL locationUrl = source.getLocation();
                if (locationUrl != null) {
                    final String normalizedLocationUrl = locationUrl.toString().replace('\\', '/');
                    int separatorIndex = normalizedLocationUrl.lastIndexOf("/");
                    if (separatorIndex >= 0 && separatorIndex == normalizedLocationUrl.length() - 1) {
                        separatorIndex = normalizedLocationUrl.lastIndexOf("/", separatorIndex - 1);
                    }
                    return normalizedLocationUrl.substring(separatorIndex + 1);
                }
            }
        } catch (final Exception ignored) {
            // Do nothing
        }
        return "?";
    }

    private static String getVersion(final Class<?> clazz) {
        final Package classPackage = clazz.getPackage();
        if (classPackage != null) {
            final String version = classPackage.getImplementationVersion();
            if (version != null) {
                return version;
            }
        }
        return "?";
    }

    @Override
    public String toString() {
        return text;
    }
}
