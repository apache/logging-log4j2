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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@code DeleteAction} class.
 */
public class DeleteActionTest {

    private static DeleteAction createAnyFilter(final String path, final boolean followLinks, final int maxDepth, final boolean testMode) {
        final PathCondition[] pathFilters = {new FixedCondition(true)};
        return create(path, followLinks, maxDepth, testMode, pathFilters);
    }

    private static DeleteAction create(final String path, final boolean followLinks, final int maxDepth, final boolean testMode,
            final PathCondition[] conditions) {
        final Configuration config = new BasicConfigurationFactory.BasicConfiguration();
        return DeleteAction.createDeleteAction(path, followLinks, maxDepth, testMode, null, conditions,
                null, config);
    }

    @Test
    public void testGetBasePathResolvesLookups() {
        final DeleteAction delete = createAnyFilter("${sys:user.home}/a/b/c", false, 1, false);

        final Path actual = delete.getBasePath();
        final String expected = System.getProperty("user.home") + "/a/b/c";

        assertThat(actual).isEqualTo(FileSystems.getDefault().getPath(expected));
    }

    @Test
    public void testGetBasePathStringReturnsOriginalParam() {
        final DeleteAction delete = createAnyFilter("${sys:user.home}/a/b/c", false, 1, false);
        assertThat(delete.getBasePathString()).isEqualTo("${sys:user.home}/a/b/c");
    }

    @Test
    public void testGetMaxDepthReturnsConstructorValue() {
        final DeleteAction delete = createAnyFilter("any", false, 23, false);
        assertThat(delete.getMaxDepth()).isEqualTo(23);
    }

    @Test
    public void testGetOptionsReturnsEmptySetIfNotFollowingLinks() {
        final DeleteAction delete = createAnyFilter("any", false, 0, false);
        assertThat(delete.getOptions()).isEqualTo(Collections.emptySet());
    }

    @Test
    public void testGetOptionsReturnsSetWithFollowLinksIfFollowingLinks() {
        final DeleteAction delete = createAnyFilter("any", true, 0, false);
        assertThat(delete.getOptions()).isEqualTo(EnumSet.of(FileVisitOption.FOLLOW_LINKS));
    }

    @Test
    public void testGetFiltersReturnsConstructorValue() {
        final PathCondition[] filters = {new FixedCondition(true), new FixedCondition(false)};

        final DeleteAction delete = create("any", true, 0, false, filters);
        assertThat(delete.getPathConditions()).isEqualTo(Arrays.asList(filters));
    }

    @Test
    public void testCreateFileVisitorReturnsDeletingVisitor() {
        final DeleteAction delete = createAnyFilter("any", true, 0, false);
        final FileVisitor<Path> visitor = delete.createFileVisitor(delete.getBasePath(), delete.getPathConditions());
        assertThat(visitor).isInstanceOf(DeletingVisitor.class);
    }

    @Test
    public void testCreateFileVisitorTestModeIsActionTestMode() {
        final DeleteAction delete = createAnyFilter("any", true, 0, false);
        assertThat(delete.isTestMode()).isFalse();
        final FileVisitor<Path> visitor = delete.createFileVisitor(delete.getBasePath(), delete.getPathConditions());
        assertThat(visitor).isInstanceOf(DeletingVisitor.class);
        assertThat(((DeletingVisitor) visitor).isTestMode()).isFalse();

        final DeleteAction deleteTestMode = createAnyFilter("any", true, 0, true);
        assertThat(deleteTestMode.isTestMode()).isTrue();
        final FileVisitor<Path> testVisitor = deleteTestMode.createFileVisitor(delete.getBasePath(),
                delete.getPathConditions());
        assertThat(testVisitor).isInstanceOf(DeletingVisitor.class);
        assertThat(((DeletingVisitor) testVisitor).isTestMode()).isTrue();
    }
}
