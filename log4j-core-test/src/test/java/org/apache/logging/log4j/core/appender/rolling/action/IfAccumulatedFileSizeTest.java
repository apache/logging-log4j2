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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.core.test.appender.rolling.action.DummyFileAttributes;
import org.junit.jupiter.api.Test;

/**
 * Tests the IfAccumulatedFileSize class.
 */
class IfAccumulatedFileSizeTest {

    @Test
    void testGetThresholdBytes() {
        assertEquals(2, create("2B").getThresholdBytes());
        assertEquals(3, create("3 B").getThresholdBytes());
        assertEquals(2 * 1024, create("2KB").getThresholdBytes());
        assertEquals(3 * 1024, create("3 KB").getThresholdBytes());
        assertEquals(2 * 1024 * 1024, create("2MB").getThresholdBytes());
        assertEquals(3 * 1024 * 1024, create("3 MB").getThresholdBytes());
        assertEquals(2L * 1024 * 1024 * 1024, create("2GB").getThresholdBytes());
        assertEquals(3L * 1024 * 1024 * 1024, create("3 GB").getThresholdBytes());
        assertEquals(2L * 1024 * 1024 * 1024 * 1024, create("2TB").getThresholdBytes());
        assertEquals(3L * 1024 * 1024 * 1024 * 1024, create("3 TB").getThresholdBytes());
    }

    private static IfAccumulatedFileSize create(final String size) {
        return IfAccumulatedFileSize.createFileSizeCondition(size);
    }

    @Test
    void testNotAcceptOnExactMatch() {
        final String[] sizes = {"2KB", "3MB", "4GB", "5TB"};
        for (final String size : sizes) {
            final IfAccumulatedFileSize condition = IfAccumulatedFileSize.createFileSizeCondition(size);
            final DummyFileAttributes attribs = new DummyFileAttributes();
            attribs.size = condition.getThresholdBytes();
            assertFalse(condition.accept(null, null, attribs));
        }
    }

    @Test
    void testAcceptIfExceedThreshold() {
        final String[] sizes = {"2KB", "3MB", "4GB", "5TB"};
        for (final String size : sizes) {
            final IfAccumulatedFileSize condition = IfAccumulatedFileSize.createFileSizeCondition(size);
            final DummyFileAttributes attribs = new DummyFileAttributes();
            attribs.size = condition.getThresholdBytes() + 1;
            assertTrue(condition.accept(null, null, attribs));
        }
    }

    @Test
    void testNotAcceptIfBelowThreshold() {
        final String[] sizes = {"2KB", "3MB", "4GB", "5TB"};
        for (final String size : sizes) {
            final IfAccumulatedFileSize condition = IfAccumulatedFileSize.createFileSizeCondition(size);
            final DummyFileAttributes attribs = new DummyFileAttributes();
            attribs.size = condition.getThresholdBytes() - 1;
            assertFalse(condition.accept(null, null, attribs));
        }
    }

    @Test
    void testAcceptOnceThresholdExceeded() {
        final DummyFileAttributes attribs = new DummyFileAttributes();
        final String[] sizes = {"2KB", "3MB", "4GB", "5TB"};
        for (final String size : sizes) {
            final IfAccumulatedFileSize condition = IfAccumulatedFileSize.createFileSizeCondition(size);
            final long quarter = condition.getThresholdBytes() / 4;
            attribs.size = quarter;
            assertFalse(condition.accept(null, null, attribs));
            assertFalse(condition.accept(null, null, attribs));
            assertFalse(condition.accept(null, null, attribs));
            assertFalse(condition.accept(null, null, attribs));
            assertTrue(condition.accept(null, null, attribs));
        }
    }

    @Test
    void testAcceptCallsNestedConditionsOnlyIfPathAccepted() {
        final CountingCondition counter = new CountingCondition(true);
        final IfAccumulatedFileSize condition = IfAccumulatedFileSize.createFileSizeCondition("2KB", counter);
        final DummyFileAttributes attribs = new DummyFileAttributes();

        final long quarter = condition.getThresholdBytes() / 4;
        attribs.size = quarter;
        assertFalse(condition.accept(null, null, attribs));
        assertEquals(0, counter.getAcceptCount());

        assertFalse(condition.accept(null, null, attribs));
        assertEquals(0, counter.getAcceptCount());

        assertFalse(condition.accept(null, null, attribs));
        assertEquals(0, counter.getAcceptCount());

        assertFalse(condition.accept(null, null, attribs));
        assertEquals(0, counter.getAcceptCount());

        assertTrue(condition.accept(null, null, attribs));
        assertEquals(1, counter.getAcceptCount());

        assertTrue(condition.accept(null, null, attribs));
        assertEquals(2, counter.getAcceptCount());
    }

    @Test
    void testBeforeTreeWalk() {
        final CountingCondition counter = new CountingCondition(true);
        final IfAccumulatedFileSize filter =
                IfAccumulatedFileSize.createFileSizeCondition("2GB", counter, counter, counter);
        filter.beforeFileTreeWalk();
        assertEquals(3, counter.getBeforeFileTreeWalkCount());
    }
}
