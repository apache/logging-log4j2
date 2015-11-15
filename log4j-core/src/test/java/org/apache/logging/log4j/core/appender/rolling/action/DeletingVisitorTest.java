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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.core.appender.rolling.action.DeletingVisitor;
import org.apache.logging.log4j.core.appender.rolling.action.PathCondition;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the {@code DeletingVisitor} class.
 */
public class DeletingVisitorTest {
    /**
     * Modifies {@code DeletingVisitor} for testing:
     * instead of actually deleting a file, it adds the path to a list for later verification.
     */
    static class DeletingVisitorHelper extends DeletingVisitor {
        List<Path> deleted = new ArrayList<Path>();

        public DeletingVisitorHelper(final Path basePath, final List<? extends PathCondition> pathFilters) {
            super(basePath, pathFilters);
        }

        @Override
        protected void delete(final Path file) throws IOException {
            deleted.add(file);
        }
    }

    @Test
    public void testAcceptedFilesAreDeleted() throws IOException {
        Path base = FileSystems.getDefault().getPath("/a/b/c");
        final FixedCondition ACCEPT_ALL = new FixedCondition(true);
        DeletingVisitorHelper visitor = new DeletingVisitorHelper(base, Arrays.asList(ACCEPT_ALL));

        Path any = FileSystems.getDefault().getPath("/a/b/c/any");
        visitor.visitFile(any, null);
        assertTrue(visitor.deleted.contains(any));
    }

    @Test
    public void testRejectedFilesAreNotDeleted() throws IOException {
        Path base = FileSystems.getDefault().getPath("/a/b/c");
        final FixedCondition REJECT_ALL = new FixedCondition(false);
        DeletingVisitorHelper visitor = new DeletingVisitorHelper(base, Arrays.asList(REJECT_ALL));

        Path any = FileSystems.getDefault().getPath("/a/b/c/any");
        visitor.visitFile(any, null);
        assertFalse(visitor.deleted.contains(any));
    }

    @Test
    public void testAllFiltersMustAcceptOrFileIsNotDeleted() throws IOException {
        Path base = FileSystems.getDefault().getPath("/a/b/c");
        final FixedCondition ACCEPT_ALL = new FixedCondition(true);
        final FixedCondition REJECT_ALL = new FixedCondition(false);
        List<? extends PathCondition> filters = Arrays.asList(ACCEPT_ALL, ACCEPT_ALL, REJECT_ALL);
        DeletingVisitorHelper visitor = new DeletingVisitorHelper(base, filters);

        Path any = FileSystems.getDefault().getPath("/a/b/c/any");
        visitor.visitFile(any, null);
        assertFalse(visitor.deleted.contains(any));
    }

    @Test
    public void testIfAllFiltersAcceptFileIsDeleted() throws IOException {
        Path base = FileSystems.getDefault().getPath("/a/b/c");
        final FixedCondition ACCEPT_ALL = new FixedCondition(true);
        List<? extends PathCondition> filters = Arrays.asList(ACCEPT_ALL, ACCEPT_ALL, ACCEPT_ALL);
        DeletingVisitorHelper visitor = new DeletingVisitorHelper(base, filters);

        Path any = FileSystems.getDefault().getPath("/a/b/c/any");
        visitor.visitFile(any, null);
        assertTrue(visitor.deleted.contains(any));
    }

    @Test
    public void testVisitFileRelativizesAgainstBase() throws IOException {
        
        PathCondition filter = new PathCondition() {
            
            @Override
            public boolean accept(Path baseDir, Path relativePath, BasicFileAttributes attrs) {
                Path expected = FileSystems.getDefault().getPath("relative");
                assertEquals(expected, relativePath);
                return true;
            }
        };
        Path base = FileSystems.getDefault().getPath("/a/b/c");
        DeletingVisitorHelper visitor = new DeletingVisitorHelper(base, Arrays.asList(filter));

        Path child = FileSystems.getDefault().getPath("/a/b/c/relative");
        visitor.visitFile(child, null);
    }
}
