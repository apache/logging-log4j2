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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.config.plugins.processor.PluginEntry;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetSystemProperty;

/**
 * Tests the Or composite condition.
 */
@SetSystemProperty(key = "log4j2.status.entries", value = "10")
class IfAnyTest {

    @Test
    void test() {
        final PathCondition TRUE = new FixedCondition(true);
        final PathCondition FALSE = new FixedCondition(false);
        assertTrue(IfAny.createOrCondition(TRUE, TRUE).accept(null, null, null));
        assertTrue(IfAny.createOrCondition(FALSE, TRUE).accept(null, null, null));
        assertTrue(IfAny.createOrCondition(TRUE, FALSE).accept(null, null, null));
        assertFalse(IfAny.createOrCondition(FALSE, FALSE).accept(null, null, null));
    }

    @Test
    void testEmptyIsFalse() {
        assertFalse(IfAny.createOrCondition().accept(null, null, null));
    }

    @Test
    void testBeforeTreeWalk() {
        final CountingCondition counter = new CountingCondition(true);
        final IfAny or = IfAny.createOrCondition(counter, counter, counter);
        or.beforeFileTreeWalk();
        assertEquals(3, counter.getBeforeFileTreeWalkCount());
    }

    @Test
    void testCreateOrConditionCalledProgrammaticallyThrowsNPEWhenComponentsNotSpecified() {
        PathCondition[] components = null;
        assertThrows(NullPointerException.class, () -> IfAny.createOrCondition(components));
    }

    @ParameterizedTest
    @ValueSource(strings = "No components provided for IfAny")
    void testCreateOrConditionCalledByPluginBuilderReturnsNullAndLogsMessageWhenComponentsNotSpecified(
            final String expectedMessage) {
        final PluginEntry nullEntry = null;
        final PluginType<IfAny> type = new PluginType<>(nullEntry, IfAny.class, "Dummy");
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
