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
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests the IfAccumulatedFileSize class.
 */
public class IfAccumulatedFileSizeTest {

    @Test
    public void testGetThresholdBytes() {
        assertThat(create("2B").getThresholdBytes()).isEqualTo(2);
        assertThat(create("3 B").getThresholdBytes()).isEqualTo(3);
        assertThat(create("2KB").getThresholdBytes()).isEqualTo(2 * 1024);
        assertThat(create("3 KB").getThresholdBytes()).isEqualTo(3 * 1024);
        assertThat(create("2MB").getThresholdBytes()).isEqualTo(2 * 1024 * 1024);
        assertThat(create("3 MB").getThresholdBytes()).isEqualTo(3 * 1024 * 1024);
        assertThat(create("2GB").getThresholdBytes()).isEqualTo(2L * 1024 * 1024 * 1024);
        assertThat(create("3 GB").getThresholdBytes()).isEqualTo(3L * 1024 * 1024 * 1024);
    }

    private static IfAccumulatedFileSize create(final String size) {
        return IfAccumulatedFileSize.createFileSizeCondition(size);
    }

    @Test
    public void testNotAcceptOnExactMatch() {
        final String[] sizes = {"2KB", "3MB", "4GB"};
        for (final String size : sizes) {
            final IfAccumulatedFileSize condition = IfAccumulatedFileSize.createFileSizeCondition(size);
            final DummyFileAttributes attribs = new DummyFileAttributes();
            attribs.size = condition.getThresholdBytes();
            assertThat(condition.accept(null, null, attribs)).isFalse();
        }
    }

    @Test
    public void testAcceptIfExceedThreshold() {
        final String[] sizes = {"2KB", "3MB", "4GB"};
        for (final String size : sizes) {
            final IfAccumulatedFileSize condition = IfAccumulatedFileSize.createFileSizeCondition(size);
            final DummyFileAttributes attribs = new DummyFileAttributes();
            attribs.size = condition.getThresholdBytes() + 1;
            assertThat(condition.accept(null, null, attribs)).isTrue();
        }
    }

    @Test
    public void testNotAcceptIfBelowThreshold() {
        final String[] sizes = {"2KB", "3MB", "4GB"};
        for (final String size : sizes) {
            final IfAccumulatedFileSize condition = IfAccumulatedFileSize.createFileSizeCondition(size);
            final DummyFileAttributes attribs = new DummyFileAttributes();
            attribs.size = condition.getThresholdBytes() - 1;
            assertThat(condition.accept(null, null, attribs)).isFalse();
        }
    }

    @Test
    public void testAcceptOnceThresholdExceeded() {
        final DummyFileAttributes attribs = new DummyFileAttributes();
        final String[] sizes = {"2KB", "3MB", "4GB"};
        for (final String size : sizes) {
            final IfAccumulatedFileSize condition = IfAccumulatedFileSize.createFileSizeCondition(size);
            final long quarter = condition.getThresholdBytes() / 4;
            attribs.size = quarter;
            assertThat(condition.accept(null, null, attribs)).isFalse();
            assertThat(condition.accept(null, null, attribs)).isFalse();
            assertThat(condition.accept(null, null, attribs)).isFalse();
            assertThat(condition.accept(null, null, attribs)).isFalse();
            assertThat(condition.accept(null, null, attribs)).isTrue();
        }
    }

    @Test
    public void testAcceptCallsNestedConditionsOnlyIfPathAccepted() {
        final CountingCondition counter = new CountingCondition(true);
        final IfAccumulatedFileSize condition = IfAccumulatedFileSize.createFileSizeCondition("2KB", counter);
        final DummyFileAttributes attribs = new DummyFileAttributes();

        final long quarter = condition.getThresholdBytes() / 4;
        attribs.size = quarter;
        assertThat(condition.accept(null, null, attribs)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(0);

        assertThat(condition.accept(null, null, attribs)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(0);

        assertThat(condition.accept(null, null, attribs)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(0);

        assertThat(condition.accept(null, null, attribs)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(0);

        assertThat(condition.accept(null, null, attribs)).isTrue();
        assertThat(counter.getAcceptCount()).isEqualTo(1);

        assertThat(condition.accept(null, null, attribs)).isTrue();
        assertThat(counter.getAcceptCount()).isEqualTo(2);
    }

    @Test
    public void testBeforeTreeWalk() {
        final CountingCondition counter = new CountingCondition(true);
        final IfAccumulatedFileSize filter = IfAccumulatedFileSize.createFileSizeCondition("2GB", counter, counter,
                counter);
        filter.beforeFileTreeWalk();
        assertThat(counter.getBeforeFileTreeWalkCount()).isEqualTo(3);
    }

}
