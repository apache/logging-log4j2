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

import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.appender.rolling.action.DeleteAction;
import org.apache.logging.log4j.core.appender.rolling.action.DeletingVisitor;
import org.apache.logging.log4j.core.appender.rolling.action.PathFilter;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the {@code DeleteAction} class.
 */
public class DeleteActionTest {

    private static DeleteAction createAnyFilter(String path, boolean followLinks, int maxDepth) {
        PathFilter[] pathFilters = {new FixedFilter(true)};
        return create(path, followLinks, maxDepth, pathFilters);
    }

    private static DeleteAction create(String path, boolean followLinks, int maxDepth, PathFilter[] filters) {
        Configuration config = new BasicConfigurationFactory().new BasicConfiguration();
        DeleteAction delete = DeleteAction.createDeleteAction(path, followLinks, maxDepth, filters, config);
        return delete;
    }

    @Test
    public void testGetBasePathResolvesLookups() {
        DeleteAction delete = createAnyFilter("${sys:user.home}/a/b/c", false, 1);
        
        Path actual = delete.getBasePath();        
        String expected = System.getProperty("user.home") + "/a/b/c";

        assertEquals(FileSystems.getDefault().getPath(expected), actual);
    }

    @Test
    public void testGetBasePathStringReturnsOriginalParam() {
        DeleteAction delete = createAnyFilter("${sys:user.home}/a/b/c", false, 1);
        assertEquals("${sys:user.home}/a/b/c", delete.getBasePathString());
    }

    @Test
    public void testGetMaxDepthReturnsConstructorValue() {
        DeleteAction delete = createAnyFilter("any", false, 23);
        assertEquals(23, delete.getMaxDepth());
    }

    @Test
    public void testGetOptionsReturnsEmptySetIfNotFollowingLinks() {
        DeleteAction delete = createAnyFilter("any", false, 0);
        assertEquals(Collections.emptySet(), delete.getOptions());
    }

    @Test
    public void testGetOptionsReturnsSetWithFollowLinksIfFollowingLinks() {
        DeleteAction delete = createAnyFilter("any", true, 0);
        assertEquals(EnumSet.of(FileVisitOption.FOLLOW_LINKS), delete.getOptions());
    }

    @Test
    public void testGetFiltersReturnsConstructorValue() {
        PathFilter[] filters = {new FixedFilter(true), new FixedFilter(false)};
        
        DeleteAction delete = create("any", true, 0, filters);
        assertEquals(Arrays.asList(filters), delete.getPathFilters());
    }

    @Test
    public void testCreateFileVisitorReturnsDeletingVisitor() {
        DeleteAction delete = createAnyFilter("any", true, 0);
        FileVisitor<Path> visitor = delete.createFileVisitor(delete.getBasePath(), delete.getPathFilters());
        assertTrue(visitor instanceof DeletingVisitor);
    }
}
