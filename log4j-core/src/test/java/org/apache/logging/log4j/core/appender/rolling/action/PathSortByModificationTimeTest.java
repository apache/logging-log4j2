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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the {@code PathSortByModificationTime} class.
 */
public class PathSortByModificationTimeTest {

    /**
     * Test method for
     * {@link org.apache.logging.log4j.core.appender.rolling.action.PathSortByModificationTime#isRecentFirst()}.
     */
    @Test
    public void testIsRecentFirstReturnsConstructorValue() {
        assertTrue(((PathSortByModificationTime) PathSortByModificationTime.createSorter(true)).isRecentFirst());
        assertFalse(((PathSortByModificationTime) PathSortByModificationTime.createSorter(false)).isRecentFirst());
    }

    @Test
    public void testCompareRecentFirst() {
        final PathSorter sorter = PathSortByModificationTime.createSorter(true);
        final Path p1 = Paths.get("aaa");
        final Path p2 = Paths.get("bbb");
        final DummyFileAttributes a1 = new DummyFileAttributes();
        final DummyFileAttributes a2 = new DummyFileAttributes();
        a1.lastModified = FileTime.fromMillis(100);
        a2.lastModified = FileTime.fromMillis(222);
        
        assertEquals("same path, 2nd more recent", 1, sorter.compare(path(p1, a1), path(p1, a2)));
        assertEquals("path ignored, 2nd more recent", 1, sorter.compare(path(p1, a1), path(p2, a2)));
        assertEquals("path ignored, 2nd more recent", 1, sorter.compare(path(p2, a1), path(p1, a2)));
        
        assertEquals("same path, 1st more recent", -1, sorter.compare(path(p1, a2), path(p1, a1)));
        assertEquals("path ignored, 1st more recent", -1, sorter.compare(path(p1, a2), path(p2, a1)));
        assertEquals("path ignored, 1st more recent", -1, sorter.compare(path(p2, a2), path(p1, a1)));
        
        assertEquals("same path, same time", 0, sorter.compare(path(p1, a1), path(p1, a1)));
        assertEquals("p2 < p1, same time", 1, sorter.compare(path(p1, a1), path(p2, a1)));
        assertEquals("p2 < p1, same time", -1, sorter.compare(path(p2, a1), path(p1, a1)));
    }

    @Test
    public void testCompareRecentLast() {
        final PathSorter sorter = PathSortByModificationTime.createSorter(false);
        final Path p1 = Paths.get("aaa");
        final Path p2 = Paths.get("bbb");
        final DummyFileAttributes a1 = new DummyFileAttributes();
        final DummyFileAttributes a2 = new DummyFileAttributes();
        a1.lastModified = FileTime.fromMillis(100);
        a2.lastModified = FileTime.fromMillis(222);
        
        assertEquals("same path, 2nd more recent", -1, sorter.compare(path(p1, a1), path(p1, a2)));
        assertEquals("path ignored, 2nd more recent", -1, sorter.compare(path(p1, a1), path(p2, a2)));
        assertEquals("path ignored, 2nd more recent", -1, sorter.compare(path(p2, a1), path(p1, a2)));
        
        assertEquals("same path, 1st more recent", 1, sorter.compare(path(p1, a2), path(p1, a1)));
        assertEquals("path ignored, 1st more recent", 1, sorter.compare(path(p1, a2), path(p2, a1)));
        assertEquals("path ignored, 1st more recent", 1, sorter.compare(path(p2, a2), path(p1, a1)));
        
        assertEquals("same path, same time", 0, sorter.compare(path(p1, a1), path(p1, a1)));
        assertEquals("p1 < p2, same time", -1, sorter.compare(path(p1, a1), path(p2, a1)));
        assertEquals("p1 < p2, same time", 1, sorter.compare(path(p2, a1), path(p1, a1)));
    }

    private PathWithAttributes path(final Path path, final DummyFileAttributes attributes) {
        return new PathWithAttributes(path, attributes);
    }

}
