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

import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;

/**
 * Tests the FileAgeFilter class.
 */
public class IfLastModifiedTest {

    @Test
    public void testGetDurationReturnsConstructorValue() {
        final IfLastModified filter = IfLastModified.createAgeCondition(Duration.parse("P7D"));
        assertThat(filter.getAge().compareTo(Duration.parse("P7D"))).isEqualTo(0);
    }

    @Test
    public void testAcceptsIfFileAgeEqualToDuration() {
        final IfLastModified filter = IfLastModified.createAgeCondition(Duration.parse("PT33S"));
        final DummyFileAttributes attrs = new DummyFileAttributes();
        final long age = 33 * 1000;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - age);
        assertThat(filter.accept(null, null, attrs)).isTrue();
    }

    @Test
    public void testAcceptsIfFileAgeExceedsDuration() {
        final IfLastModified filter = IfLastModified.createAgeCondition(Duration.parse("PT33S"));
        final DummyFileAttributes attrs = new DummyFileAttributes();
        final long age = 33 * 1000 + 5;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - age);
        assertThat(filter.accept(null, null, attrs)).isTrue();
    }

    @Test
    public void testDoesNotAcceptIfFileAgeLessThanDuration() {
        final IfLastModified filter = IfLastModified.createAgeCondition(Duration.parse("PT33S"));
        final DummyFileAttributes attrs = new DummyFileAttributes();
        final long age = 33 * 1000 - 5;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - age);
        assertThat(filter.accept(null, null, attrs)).isFalse();
    }

    @Test
    public void testAcceptCallsNestedConditionsOnlyIfPathAccepted() {
        final CountingCondition counter = new CountingCondition(true);
        final IfLastModified filter = IfLastModified.createAgeCondition(Duration.parse("PT33S"), counter);
        final DummyFileAttributes attrs = new DummyFileAttributes();
        final long oldEnough = 33 * 1000 + 5;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - oldEnough);

        assertThat(filter.accept(null, null, attrs)).isTrue();
        assertThat(counter.getAcceptCount()).isEqualTo(1);
        assertThat(filter.accept(null, null, attrs)).isTrue();
        assertThat(counter.getAcceptCount()).isEqualTo(2);
        assertThat(filter.accept(null, null, attrs)).isTrue();
        assertThat(counter.getAcceptCount()).isEqualTo(3);
        
        final long tooYoung = 33 * 1000 - 5;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - tooYoung);
        assertThat(filter.accept(null, null, attrs)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(3); // no increase
        assertThat(filter.accept(null, null, attrs)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(3);
        assertThat(filter.accept(null, null, attrs)).isFalse();
        assertThat(counter.getAcceptCount()).isEqualTo(3);
    }

    @Test
    public void testBeforeTreeWalk() {
        final CountingCondition counter = new CountingCondition(true);
        final IfLastModified filter = IfLastModified.createAgeCondition(Duration.parse("PT33S"), counter, counter,
                counter);
        filter.beforeFileTreeWalk();
        assertThat(counter.getBeforeFileTreeWalkCount()).isEqualTo(3);
    }
}
