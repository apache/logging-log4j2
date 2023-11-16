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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.attribute.FileTime;
import java.util.List;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.plugins.processor.PluginEntry;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.test.appender.rolling.action.DummyFileAttributes;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the FileAgeFilter class.
 */
public class IfLastModifiedTest {

    @Test
    public void testGetDurationReturnsConstructorValue() {
        final IfLastModified filter = IfLastModified.createAgeCondition(Duration.parse("P7D"));
        assertEquals(0, filter.getAge().compareTo(Duration.parse("P7D")));
    }

    @Test
    public void testAcceptsIfFileAgeEqualToDuration() {
        final IfLastModified filter = IfLastModified.createAgeCondition(Duration.parse("PT33S"));
        final DummyFileAttributes attrs = new DummyFileAttributes();
        final long age = 33 * 1000;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - age);
        assertTrue(filter.accept(null, null, attrs));
    }

    @Test
    public void testAcceptsIfFileAgeExceedsDuration() {
        final IfLastModified filter = IfLastModified.createAgeCondition(Duration.parse("PT33S"));
        final DummyFileAttributes attrs = new DummyFileAttributes();
        final long age = 33 * 1000 + 5;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - age);
        assertTrue(filter.accept(null, null, attrs));
    }

    @Test
    public void testDoesNotAcceptIfFileAgeLessThanDuration() {
        final IfLastModified filter = IfLastModified.createAgeCondition(Duration.parse("PT33S"));
        final DummyFileAttributes attrs = new DummyFileAttributes();
        final long age = 33 * 1000 - 5;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - age);
        assertFalse(filter.accept(null, null, attrs));
    }

    @Test
    public void testAcceptCallsNestedConditionsOnlyIfPathAccepted() {
        final CountingCondition counter = new CountingCondition(true);
        final IfLastModified filter = IfLastModified.createAgeCondition(Duration.parse("PT33S"), counter);
        final DummyFileAttributes attrs = new DummyFileAttributes();
        final long oldEnough = 33 * 1000 + 5;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - oldEnough);

        assertTrue(filter.accept(null, null, attrs));
        assertEquals(1, counter.getAcceptCount());
        assertTrue(filter.accept(null, null, attrs));
        assertEquals(2, counter.getAcceptCount());
        assertTrue(filter.accept(null, null, attrs));
        assertEquals(3, counter.getAcceptCount());

        final long tooYoung = 33 * 1000 - 5;
        attrs.lastModified = FileTime.fromMillis(System.currentTimeMillis() - tooYoung);
        assertFalse(filter.accept(null, null, attrs));
        assertEquals(3, counter.getAcceptCount()); // no increase
        assertFalse(filter.accept(null, null, attrs));
        assertEquals(3, counter.getAcceptCount());
        assertFalse(filter.accept(null, null, attrs));
        assertEquals(3, counter.getAcceptCount());
    }

    @Test
    public void testBeforeTreeWalk() {
        final CountingCondition counter = new CountingCondition(true);
        final IfLastModified filter =
                IfLastModified.createAgeCondition(Duration.parse("PT33S"), counter, counter, counter);
        filter.beforeFileTreeWalk();
        assertEquals(3, counter.getBeforeFileTreeWalkCount());
    }

    @Test
    public void testCreateAgeConditionCalledProgrammaticallyThrowsNPEWhenAgeIsNotSpecified() {
        Duration age = null;
        assertThrows(NullPointerException.class, () -> IfLastModified.createAgeCondition(age));
    }

    @ParameterizedTest
    @ValueSource(strings = "No age provided for IfLastModified")
    public void testCreateAgeConditionCalledByPluginBuilderReturnsNullAndLogsMessageWhenAgeIsNotSpecified(
            final String expectedMessage) {
        final PluginEntry nullEntry = null;
        final PluginType<IfLastModified> type = new PluginType<>(nullEntry, IfLastModified.class, "Dummy");
        final PluginBuilder builder = new PluginBuilder(type)
                .withConfiguration(new NullConfiguration())
                .withConfigurationNode(new Node());
        final Object asBuilt = builder.build();
        final List<StatusData> loggerStatusData = StatusLogger.getLogger().getStatusData();

        assertNull(asBuilt);
        assertTrue(
                loggerStatusData.stream().anyMatch(e -> e.getFormattedStatus().contains(expectedMessage)));
    }
}
