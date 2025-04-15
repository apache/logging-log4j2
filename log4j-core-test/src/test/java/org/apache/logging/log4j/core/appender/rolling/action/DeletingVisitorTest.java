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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@code DeletingVisitor} class.
 */
class DeletingVisitorTest {
    /**
     * Modifies {@code DeletingVisitor} for testing: instead of actually deleting a file, it adds the path to a list for
     * later verification.
     */
    static class DeletingVisitorHelper extends DeletingVisitor {
        List<Path> deleted = new ArrayList<>();

        public DeletingVisitorHelper(
                final Path basePath, final List<? extends PathCondition> pathFilters, final boolean testMode) {
            super(basePath, pathFilters, testMode);
        }

        @Override
        protected void delete(final Path file) {
            deleted.add(file); // overrides and stores path instead of deleting
        }
    }

    @Test
    void testAcceptedFilesAreDeleted() throws IOException {
        final Path base = Paths.get("/a/b/c");
        final FixedCondition ACCEPT_ALL = new FixedCondition(true);
        final DeletingVisitorHelper visitor =
                new DeletingVisitorHelper(base, Collections.singletonList(ACCEPT_ALL), false);

        final Path any = Paths.get("/a/b/c/any");
        visitor.visitFile(any, null);
        assertTrue(visitor.deleted.contains(any));
    }

    @Test
    void testRejectedFilesAreNotDeleted() throws IOException {
        final Path base = Paths.get("/a/b/c");
        final FixedCondition REJECT_ALL = new FixedCondition(false);
        final DeletingVisitorHelper visitor =
                new DeletingVisitorHelper(base, Collections.singletonList(REJECT_ALL), false);

        final Path any = Paths.get("/a/b/c/any");
        visitor.visitFile(any, null);
        assertFalse(visitor.deleted.contains(any));
    }

    @Test
    void testAllFiltersMustAcceptOrFileIsNotDeleted() throws IOException {
        final Path base = Paths.get("/a/b/c");
        final FixedCondition ACCEPT_ALL = new FixedCondition(true);
        final FixedCondition REJECT_ALL = new FixedCondition(false);
        final List<? extends PathCondition> filters = Arrays.asList(ACCEPT_ALL, ACCEPT_ALL, REJECT_ALL);
        final DeletingVisitorHelper visitor = new DeletingVisitorHelper(base, filters, false);

        final Path any = Paths.get("/a/b/c/any");
        visitor.visitFile(any, null);
        assertFalse(visitor.deleted.contains(any));
    }

    @Test
    void testIfAllFiltersAcceptFileIsDeleted() throws IOException {
        final Path base = Paths.get("/a/b/c");
        final FixedCondition ACCEPT_ALL = new FixedCondition(true);
        final List<? extends PathCondition> filters = Arrays.asList(ACCEPT_ALL, ACCEPT_ALL, ACCEPT_ALL);
        final DeletingVisitorHelper visitor = new DeletingVisitorHelper(base, filters, false);

        final Path any = Paths.get("/a/b/c/any");
        visitor.visitFile(any, null);
        assertTrue(visitor.deleted.contains(any));
    }

    @Test
    void testInTestModeFileIsNotDeletedEvenIfAllFiltersAccept() throws IOException {
        final Path base = Paths.get("/a/b/c");
        final FixedCondition ACCEPT_ALL = new FixedCondition(true);
        final List<? extends PathCondition> filters = Arrays.asList(ACCEPT_ALL, ACCEPT_ALL, ACCEPT_ALL);
        final DeletingVisitorHelper visitor = new DeletingVisitorHelper(base, filters, true);

        final Path any = Paths.get("/a/b/c/any");
        visitor.visitFile(any, null);
        assertFalse(visitor.deleted.contains(any));
    }

    @Test
    void testVisitFileRelativizesAgainstBase() throws IOException {

        final PathCondition filter = new PathCondition() {

            @Override
            public boolean accept(final Path baseDir, final Path relativePath, final BasicFileAttributes attrs) {
                final Path expected = Paths.get("relative");
                assertEquals(expected, relativePath);
                return true;
            }

            @Override
            public void beforeFileTreeWalk() {}
        };
        final Path base = Paths.get("/a/b/c");
        final DeletingVisitorHelper visitor = new DeletingVisitorHelper(base, Collections.singletonList(filter), false);

        final Path child = Paths.get("/a/b/c/relative");
        visitor.visitFile(child, null);
    }

    @Test
    void testNoSuchFileFailure() throws IOException {
        final DeletingVisitorHelper visitor =
                new DeletingVisitorHelper(Paths.get("/a/b/c"), Collections.emptyList(), true);
        assertEquals(
                FileVisitResult.CONTINUE,
                visitor.visitFileFailed(Paths.get("doesNotExist"), new NoSuchFileException("doesNotExist")));
    }

    @Test
    void testIOException() {
        final DeletingVisitorHelper visitor =
                new DeletingVisitorHelper(Paths.get("/a/b/c"), Collections.emptyList(), true);
        final IOException exception = new IOException();
        try {
            visitor.visitFileFailed(Paths.get("doesNotExist"), exception);
            fail();
        } catch (IOException e) {
            assertSame(exception, e);
        }
    }
}
