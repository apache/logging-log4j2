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

import java.nio.file.attribute.FileTime;

import org.apache.logging.log4j.core.appender.rolling.action.Duration;
import org.apache.logging.log4j.core.appender.rolling.action.FileLastModifiedFilter;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the FileAgeFilter class.
 */
public class FileLastModifiedFilterTest {

    @Test
    public void testGetDurationReturnsConstructorValue() {
        FileLastModifiedFilter filter = FileLastModifiedFilter.createAgeFilter(Duration.parse("P7D"));
        assertEquals(0, filter.getDuration().compareTo(Duration.parse("P7D")));
    }

    @Test
    public void testAcceptsIfFileAgeEqualToDuration() {
        FileLastModifiedFilter filter = FileLastModifiedFilter.createAgeFilter(Duration.parse("PT33S"));
        DummyFileAttributes attrs = new DummyFileAttributes();
        final long age = 33 * 1000;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - age);
        assertTrue(filter.accept(null, null, attrs));
    }

    @Test
    public void testAcceptsIfFileAgeExceedsDuration() {
        FileLastModifiedFilter filter = FileLastModifiedFilter.createAgeFilter(Duration.parse("PT33S"));
        DummyFileAttributes attrs = new DummyFileAttributes();
        final long age = 33 * 1000 + 5;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - age);
        assertTrue(filter.accept(null, null, attrs));
    }

    @Test
    public void testDoesNotAcceptIfFileAgeLessThanDuration() {
        FileLastModifiedFilter filter = FileLastModifiedFilter.createAgeFilter(Duration.parse("PT33S"));
        DummyFileAttributes attrs = new DummyFileAttributes();
        final long age = 33 * 1000 - 5;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - age);
        assertFalse(filter.accept(null, null, attrs));
    }
}
