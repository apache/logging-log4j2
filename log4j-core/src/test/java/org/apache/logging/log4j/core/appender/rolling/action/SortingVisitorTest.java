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
        Files.setLastModifiedTime(aaa, FileTime.fromMillis(System.currentTimeMillis()));
        
        Thread.sleep(1);
        bbb = Files.createTempFile(base, "bbb", null, new FileAttribute<?>[0]);
        Files.setLastModifiedTime(bbb, FileTime.fromMillis(System.currentTimeMillis() + 1));
        
        Thread.sleep(1);
        ccc = Files.createTempFile(base, "ccc", null, new FileAttribute<?>[0]);
        Files.setLastModifiedTime(ccc, FileTime.fromMillis(System.currentTimeMillis() + 2));
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
        SortingVisitor visitor = new SortingVisitor(new PathSortByModificationTime(true));
        Set<FileVisitOption> options = Collections.emptySet();
        Files.walkFileTree(base, options, 1, visitor);

        List<PathWithAttributes> found = visitor.getSortedPaths();
        assertNotNull(found);
        assertEquals("file count", 3, found.size());
        assertEquals("1st: most recent", ccc, found.get(0).getPath());
        assertEquals("2nd", bbb, found.get(1).getPath());
        assertEquals("3rd: oldest", aaa, found.get(2).getPath());
    }

    @Test
    public void testRecentLast() throws Exception {
        SortingVisitor visitor = new SortingVisitor(new PathSortByModificationTime(false));
        Set<FileVisitOption> options = Collections.emptySet();
        Files.walkFileTree(base, options, 1, visitor);

        List<PathWithAttributes> found = visitor.getSortedPaths();
        assertNotNull(found);
        assertEquals("file count", 3, found.size());
        assertEquals("1st: oldest first", aaa, found.get(0).getPath());
        assertEquals("2nd", bbb, found.get(1).getPath());
        assertEquals("3rd: most recent last", ccc, found.get(2).getPath());
    }
}
