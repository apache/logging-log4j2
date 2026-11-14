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
package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.DynamicThresholdFilter;
import org.apache.logging.log4j.core.filter.MarkerFilter;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junitpioneer.jupiter.Issue;

class CompositeConfigurationFilterTest {

    private static final String THRESHOLD_FILTER =
            "<ThresholdFilter level=\"TRACE\" onMatch=\"NEUTRAL\" onMismatch=\"DENY\"/>";
    private static final String MARKER_FILTER =
            "<MarkerFilter marker=\"blocked\" onMatch=\"DENY\" onMismatch=\"NEUTRAL\"/>";
    private static final String DYNAMIC_FILTER = "<DynamicThresholdFilter key=\"loginId\" defaultThreshold=\"ERROR\""
            + " onMatch=\"ACCEPT\" onMismatch=\"DENY\">"
            + "<KeyValuePair key=\"alice\" value=\"DEBUG\"/>"
            + "<KeyValuePair key=\"bob\" value=\"INFO\"/>"
            + "</DynamicThresholdFilter>";

    @ParameterizedTest(name = "targetFilters={0}, sourceGroup={1}")
    @CsvSource({"0, false", "1, false", "2, false", "0, true", "1, true", "2, true"})
    @Issue("https://github.com/apache/logging-log4j2/issues/3173")
    void preservesFilterChildren(final int targetFilterCount, final boolean sourceGroup) throws Exception {
        String targetFilters = targetFilterCount == 0 ? "" : THRESHOLD_FILTER;
        if (targetFilterCount == 2) {
            targetFilters = "<Filters>" + targetFilters + MARKER_FILTER + "</Filters>";
        }
        final String sourceFilters = sourceGroup ? "<Filters>" + DYNAMIC_FILTER + "</Filters>" : DYNAMIC_FILTER;

        try (final LoggerContext context = new LoggerContext("compositeFilters")) {
            final CompositeConfiguration configuration = new CompositeConfiguration(
                    Arrays.asList(xmlConfiguration(context, targetFilters), xmlConfiguration(context, sourceFilters)));
            context.start(configuration);

            final Filter filter = configuration.getFilter();
            Filter sourceFilter = filter;
            if (targetFilterCount > 0) {
                final CompositeFilter merged = assertInstanceOf(CompositeFilter.class, filter);
                assertEquals(targetFilterCount + 1, merged.size());
                final ThresholdFilter threshold = assertInstanceOf(ThresholdFilter.class, merged.getFiltersArray()[0]);
                assertEquals(Level.TRACE, threshold.getLevel());
                assertEquals(Filter.Result.NEUTRAL, threshold.getOnMatch());
                assertEquals(Filter.Result.DENY, threshold.getOnMismatch());
                if (targetFilterCount == 2) {
                    assertInstanceOf(MarkerFilter.class, merged.getFiltersArray()[1]);
                }
                sourceFilter = merged.getFiltersArray()[targetFilterCount];
            }
            if (sourceGroup) {
                final CompositeFilter group = assertInstanceOf(CompositeFilter.class, sourceFilter);
                assertEquals(1, group.size());
                sourceFilter = group.getFiltersArray()[0];
            }
            final DynamicThresholdFilter dynamic = assertInstanceOf(DynamicThresholdFilter.class, sourceFilter);
            assertAll(
                    () -> assertEquals("loginId", dynamic.getKey()),
                    () -> assertEquals(2, dynamic.getLevelMap().size()),
                    () -> assertEquals(Level.DEBUG, dynamic.getLevelMap().get("alice")),
                    () -> assertEquals(Level.INFO, dynamic.getLevelMap().get("bob")),
                    () -> assertEquals(Filter.Result.ACCEPT, filter.filter(event("alice", Level.DEBUG))),
                    () -> assertEquals(Filter.Result.DENY, filter.filter(event("bob", Level.DEBUG))),
                    () -> assertEquals(Filter.Result.ACCEPT, filter.filter(event("bob", Level.INFO))),
                    () -> assertEquals(Filter.Result.DENY, filter.filter(event("unknown", Level.INFO))),
                    () -> assertEquals(Filter.Result.ACCEPT, filter.filter(event("unknown", Level.ERROR))));
        }
    }

    private static XmlConfiguration xmlConfiguration(final LoggerContext context, final String filters)
            throws Exception {
        final String xml = "<Configuration>" + filters + "<Loggers><Root level=\"ALL\"/></Loggers></Configuration>";
        return new XmlConfiguration(
                context, new ConfigurationSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
    }

    private static LogEvent event(final String loginId, final Level level) {
        final SortedArrayStringMap contextData = new SortedArrayStringMap();
        contextData.putValue("loginId", loginId);
        return Log4jLogEvent.newBuilder()
                .setLevel(level)
                .setContextData(contextData)
                .build();
    }
}
