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
package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Tuple of a {@code Path} and {@code BasicFileAttributes}, used for sorting.
 */
public class PathWithAttributes {

    private final Path path;
    private final BasicFileAttributes attributes;

    public PathWithAttributes(final Path path, final BasicFileAttributes attributes) {
        this.path = Objects.requireNonNull(path, "path");
        this.attributes = Objects.requireNonNull(attributes, "attributes");
    }

    @Override
    public String toString() {
        return path + " (modified: " + attributes.lastModifiedTime() + ")";
    }

    /**
     * Returns the path.
     *
     * @return the path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the attributes.
     *
     * @return the attributes
     */
    public BasicFileAttributes getAttributes() {
        return attributes;
    }
}
