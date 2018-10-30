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

package org.apache.logging.log4j.core.appender.rolling.action;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * FileVisitor that sorts files.
 */
public class SortingVisitor extends SimpleFileVisitor<Path> {

    private final PathSorter sorter;
    private final List<PathWithAttributes> collected = new ArrayList<>();

    /**
     * Constructs a new DeletingVisitor.
     * 
     * @param basePath used to relativize paths
     * @param pathFilters objects that need to confirm whether a file can be deleted
     */
    public SortingVisitor(final PathSorter sorter) {
        this.sorter = Objects.requireNonNull(sorter, "sorter");
    }

    @Override
    public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) throws IOException {
        collected.add(new PathWithAttributes(path, attrs));
        return FileVisitResult.CONTINUE;
    }
    
    public List<PathWithAttributes> getSortedPaths() {
        Collections.sort(collected, sorter);
        return collected;
    }
}
