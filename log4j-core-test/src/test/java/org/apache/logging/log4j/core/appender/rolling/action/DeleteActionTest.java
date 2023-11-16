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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.test.BasicConfigurationFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@code DeleteAction} class.
 */
public class DeleteActionTest {

    private static DeleteAction createAnyFilter(
            final String path, final boolean followLinks, final int maxDepth, final boolean testMode) {
        final PathCondition[] pathFilters = {new FixedCondition(true)};
        return create(path, followLinks, maxDepth, testMode, pathFilters);
    }

    private static DeleteAction create(
            final String path,
            final boolean followLinks,
            final int maxDepth,
            final boolean testMode,
            final PathCondition[] conditions) {
        final Configuration config = new BasicConfigurationFactory().new BasicConfiguration();
        final DeleteAction delete =
                DeleteAction.createDeleteAction(path, followLinks, maxDepth, testMode, null, conditions, null, config);
        return delete;
    }

    @Test
    public void testGetBasePathResolvesLookups() {
        final DeleteAction delete = createAnyFilter("${sys:user.home}/a/b/c", false, 1, false);

        final Path actual = delete.getBasePath();
        final String expected = System.getProperty("user.home") + "/a/b/c";

        assertEquals(FileSystems.getDefault().getPath(expected), actual);
    }

    @Test
    public void testGetBasePathStringReturnsOriginalParam() {
        final DeleteAction delete = createAnyFilter("${sys:user.home}/a/b/c", false, 1, false);
        assertEquals("${sys:user.home}/a/b/c", delete.getBasePathString());
    }

    @Test
    public void testGetMaxDepthReturnsConstructorValue() {
        final DeleteAction delete = createAnyFilter("any", false, 23, false);
        assertEquals(23, delete.getMaxDepth());
    }

    @Test
    public void testGetOptionsReturnsEmptySetIfNotFollowingLinks() {
        final DeleteAction delete = createAnyFilter("any", false, 0, false);
        assertEquals(Collections.emptySet(), delete.getOptions());
    }

    @Test
    public void testGetOptionsReturnsSetWithFollowLinksIfFollowingLinks() {
        final DeleteAction delete = createAnyFilter("any", true, 0, false);
        assertEquals(EnumSet.of(FileVisitOption.FOLLOW_LINKS), delete.getOptions());
    }

    @Test
    public void testGetFiltersReturnsConstructorValue() {
        final PathCondition[] filters = {new FixedCondition(true), new FixedCondition(false)};

        final DeleteAction delete = create("any", true, 0, false, filters);
        assertEquals(Arrays.asList(filters), delete.getPathConditions());
    }

    @Test
    public void testCreateFileVisitorReturnsDeletingVisitor() {
        final DeleteAction delete = createAnyFilter("any", true, 0, false);
        final FileVisitor<Path> visitor = delete.createFileVisitor(delete.getBasePath(), delete.getPathConditions());
        assertThat(visitor, instanceOf(DeletingVisitor.class));
    }

    @Test
    public void testCreateFileVisitorTestModeIsActionTestMode() {
        final DeleteAction delete = createAnyFilter("any", true, 0, false);
        assertFalse(delete.isTestMode());
        final FileVisitor<Path> visitor = delete.createFileVisitor(delete.getBasePath(), delete.getPathConditions());
        assertThat(visitor, instanceOf(DeletingVisitor.class));
        assertFalse(((DeletingVisitor) visitor).isTestMode());

        final DeleteAction deleteTestMode = createAnyFilter("any", true, 0, true);
        assertTrue(deleteTestMode.isTestMode());
        final FileVisitor<Path> testVisitor =
                deleteTestMode.createFileVisitor(delete.getBasePath(), delete.getPathConditions());
        assertThat(testVisitor, instanceOf(DeletingVisitor.class));
        assertTrue(((DeletingVisitor) testVisitor).isTestMode());
    }
}
