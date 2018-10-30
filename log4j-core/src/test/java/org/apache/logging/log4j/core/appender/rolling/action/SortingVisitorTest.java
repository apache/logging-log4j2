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

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the SortingVisitor class.
 */
public class SortingVisitorTest {
    
    private Path base;
    private Path aaa;
    private Path bbb;
    private Path ccc;

    @Before
    public void setUp() throws Exception {
        base = Files.createTempDirectory("tempDir", new FileAttribute<?>[0]);
        aaa = Files.createTempFile(base, "aaa", null, new FileAttribute<?>[0]);
        bbb = Files.createTempFile(base, "bbb", null, new FileAttribute<?>[0]);
        ccc = Files.createTempFile(base, "ccc", null, new FileAttribute<?>[0]);
        
        // lastModified granularity is 1 sec(!) on some file systems...
        final long now = System.currentTimeMillis();
        Files.setLastModifiedTime(aaa, FileTime.fromMillis(now));
        Files.setLastModifiedTime(bbb, FileTime.fromMillis(now + 1000));
        Files.setLastModifiedTime(ccc, FileTime.fromMillis(now + 2000));
    }
    
    @After
    public void tearDown() throws Exception {
        Files.deleteIfExists(ccc);
        Files.deleteIfExists(bbb);
        Files.deleteIfExists(aaa);
        Files.deleteIfExists(base);
    }

    @Test
    public void testRecentFirst() throws Exception {
        final SortingVisitor visitor = new SortingVisitor(new PathSortByModificationTime(true));
        final Set<FileVisitOption> options = Collections.emptySet();
        Files.walkFileTree(base, options, 1, visitor);

        final List<PathWithAttributes> found = visitor.getSortedPaths();
        assertNotNull(found);
        assertEquals("file count", 3, found.size());
        assertEquals("1st: most recent; sorted=" + found, ccc, found.get(0).getPath());
        assertEquals("2nd; sorted=" + found, bbb, found.get(1).getPath());
        assertEquals("3rd: oldest; sorted=" + found, aaa, found.get(2).getPath());
    }

    @Test
    public void testRecentLast() throws Exception {
        final SortingVisitor visitor = new SortingVisitor(new PathSortByModificationTime(false));
        final Set<FileVisitOption> options = Collections.emptySet();
        Files.walkFileTree(base, options, 1, visitor);

        final List<PathWithAttributes> found = visitor.getSortedPaths();
        assertNotNull(found);
        assertEquals("file count", 3, found.size());
        assertEquals("1st: oldest first; sorted=" + found, aaa, found.get(0).getPath());
        assertEquals("2nd; sorted=" + found, bbb, found.get(1).getPath());
        assertEquals("3rd: most recent sorted; list=" + found, ccc, found.get(2).getPath());
    }
}
