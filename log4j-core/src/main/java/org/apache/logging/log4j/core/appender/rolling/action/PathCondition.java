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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

/**
 * Filter that accepts or rejects a candidate {@code Path} for deletion.
 */
public interface PathCondition {

    /**
     * The empty array.
     */
    static final PathCondition[] EMPTY_ARRAY = {};

    /**
     * Copies the given input.
     *
     * @param source What to copy
     * @return a copy, never null.
     */
    static PathCondition[] copy(PathCondition... source) {
        return source == null || source.length == 0 ? EMPTY_ARRAY : Arrays.copyOf(source, source.length);
    }

    /**
     * Invoked before a new {@linkplain Files#walkFileTree(Path, java.util.Set, int, java.nio.file.FileVisitor) file
     * tree walk} is started. Stateful PathConditions can reset their state when this method is called.
     */
    void beforeFileTreeWalk();

    /**
     * Returns {@code true} if the specified candidate path should be deleted, {@code false} otherwise.
     *
     * @param baseDir the directory from where to start scanning for deletion candidate files
     * @param relativePath the candidate for deletion. This path is relative to the baseDir.
     * @param attrs attributes of the candidate path
     * @return whether the candidate path should be deleted
     */
    boolean accept(final Path baseDir, final Path relativePath, final BasicFileAttributes attrs);
}
