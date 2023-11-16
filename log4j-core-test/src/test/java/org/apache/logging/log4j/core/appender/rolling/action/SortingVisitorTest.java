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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the SortingVisitor class.
 */
public class SortingVisitorTest {

    @TempDir
    Path base;

    private Path aaa;
    private Path bbb;
    private Path ccc;

    @BeforeEach
    public void setUp() throws Exception {
        aaa = Files.createFile(base.resolve("aaa"));
        bbb = Files.createFile(base.resolve("bbb"));
        ccc = Files.createFile(base.resolve("ccc"));

        // lastModified granularity is 1 sec(!) on some file systems...
        final long now = System.currentTimeMillis();
        Files.setLastModifiedTime(aaa, FileTime.fromMillis(now));
        Files.setLastModifiedTime(bbb, FileTime.fromMillis(now + 1000));
        Files.setLastModifiedTime(ccc, FileTime.fromMillis(now + 2000));
    }

    @Test
    public void testRecentFirst() throws Exception {
        final SortingVisitor visitor = new SortingVisitor(new PathSortByModificationTime(true));
        final Set<FileVisitOption> options = Collections.emptySet();
        Files.walkFileTree(base, options, 1, visitor);

        final List<PathWithAttributes> found = visitor.getSortedPaths();
        assertNotNull(found);
        assertEquals(3, found.size(), "file count");
        assertEquals(ccc, found.get(0).getPath(), "1st: most recent; sorted=" + found);
        assertEquals(bbb, found.get(1).getPath(), "2nd; sorted=" + found);
        assertEquals(aaa, found.get(2).getPath(), "3rd: oldest; sorted=" + found);
    }

    @Test
    public void testRecentLast() throws Exception {
        final SortingVisitor visitor = new SortingVisitor(new PathSortByModificationTime(false));
        final Set<FileVisitOption> options = Collections.emptySet();
        Files.walkFileTree(base, options, 1, visitor);

        final List<PathWithAttributes> found = visitor.getSortedPaths();
        assertNotNull(found);
        assertEquals(3, found.size(), "file count");
        assertEquals(aaa, found.get(0).getPath(), "1st: oldest first; sorted=" + found);
        assertEquals(bbb, found.get(1).getPath(), "2nd; sorted=" + found);
        assertEquals(ccc, found.get(2).getPath(), "3rd: most recent sorted; list=" + found);
    }

    @Test
    public void testNoSuchFileFailure() throws IOException {
        final SortingVisitor visitor = new SortingVisitor(new PathSortByModificationTime(false));
        assertSame(
                FileVisitResult.CONTINUE,
                visitor.visitFileFailed(Paths.get("doesNotExist"), new NoSuchFileException("doesNotExist")));
    }

    @Test
    public void testIOException() {
        final SortingVisitor visitor = new SortingVisitor(new PathSortByModificationTime(false));
        final IOException exception = new IOException();
        assertSame(
                exception,
                assertThrows(IOException.class, () -> visitor.visitFileFailed(Paths.get("doesNotExist"), exception)));
    }
}
