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
package org.apache.log4j.builders.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.Properties;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.spi.Filter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class LevelRangeFilterBuilderTest {

    @ParameterizedTest
    @ArgumentsSource(TestLevelRangeFilterBuilderProvider.class)
    public void testAcceptOnMatchTrue(final TestLevelRangeFilterBuilder builder) throws Exception {
        final LevelRangeFilter levelRangeFilter = builder.build(Level.INFO, Level.ERROR, true);

        assertResult(Result.DENY, levelRangeFilter, Level.ALL);
        assertResult(Result.DENY, levelRangeFilter, Level.DEBUG);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.INFO);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.WARN);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.ERROR);
        assertResult(Result.DENY, levelRangeFilter, Level.FATAL);
        assertResult(Result.DENY, levelRangeFilter, Level.OFF);
    }

    @ParameterizedTest
    @ArgumentsSource(TestLevelRangeFilterBuilderProvider.class)
    public void testAcceptOnMatchFalse(final TestLevelRangeFilterBuilder builder) throws Exception {
        final LevelRangeFilter levelRangeFilter = builder.build(Level.INFO, Level.ERROR, false);

        assertResult(Result.DENY, levelRangeFilter, Level.ALL);
        assertResult(Result.DENY, levelRangeFilter, Level.DEBUG);
        assertResult(Result.NEUTRAL, levelRangeFilter, Level.INFO);
        assertResult(Result.NEUTRAL, levelRangeFilter, Level.WARN);
        assertResult(Result.NEUTRAL, levelRangeFilter, Level.ERROR);
        assertResult(Result.DENY, levelRangeFilter, Level.FATAL);
        assertResult(Result.DENY, levelRangeFilter, Level.OFF);
    }

    @ParameterizedTest
    @ArgumentsSource(TestLevelRangeFilterBuilderProvider.class)
    public void testAcceptOnMatchNull(final TestLevelRangeFilterBuilder builder) throws Exception {
        final LevelRangeFilter levelRangeFilter = builder.build(Level.INFO, Level.ERROR, null);

        assertResult(Result.DENY, levelRangeFilter, Level.ALL);
        assertResult(Result.DENY, levelRangeFilter, Level.DEBUG);
        assertResult(Result.NEUTRAL, levelRangeFilter, Level.INFO);
        assertResult(Result.NEUTRAL, levelRangeFilter, Level.WARN);
        assertResult(Result.NEUTRAL, levelRangeFilter, Level.ERROR);
        assertResult(Result.DENY, levelRangeFilter, Level.FATAL);
        assertResult(Result.DENY, levelRangeFilter, Level.OFF);
    }

    @ParameterizedTest
    @ArgumentsSource(TestLevelRangeFilterBuilderProvider.class)
    public void testMinLevelNull(final TestLevelRangeFilterBuilder builder) throws Exception {
        final LevelRangeFilter levelRangeFilter = builder.build(null, Level.ERROR, true);

        assertResult(Result.ACCEPT, levelRangeFilter, Level.ALL);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.DEBUG);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.INFO);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.WARN);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.ERROR);
        assertResult(Result.DENY, levelRangeFilter, Level.FATAL);
        assertResult(Result.DENY, levelRangeFilter, Level.OFF);
    }

    @ParameterizedTest
    @ArgumentsSource(TestLevelRangeFilterBuilderProvider.class)
    public void testMaxLevelNull(final TestLevelRangeFilterBuilder builder) throws Exception {
        final LevelRangeFilter levelRangeFilter = builder.build(Level.INFO, null, true);

        assertResult(Result.DENY, levelRangeFilter, Level.ALL);
        assertResult(Result.DENY, levelRangeFilter, Level.DEBUG);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.INFO);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.WARN);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.ERROR);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.FATAL);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.OFF);
    }

    @ParameterizedTest
    @ArgumentsSource(TestLevelRangeFilterBuilderProvider.class)
    public void testMinMaxLevelSame(final TestLevelRangeFilterBuilder builder) throws Exception {
        final LevelRangeFilter levelRangeFilter = builder.build(Level.INFO, Level.INFO, true);

        assertResult(Result.DENY, levelRangeFilter, Level.ALL);
        assertResult(Result.DENY, levelRangeFilter, Level.DEBUG);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.INFO);
        assertResult(Result.DENY, levelRangeFilter, Level.WARN);
        assertResult(Result.DENY, levelRangeFilter, Level.ERROR);
        assertResult(Result.DENY, levelRangeFilter, Level.FATAL);
        assertResult(Result.DENY, levelRangeFilter, Level.OFF);
    }

    @ParameterizedTest
    @ArgumentsSource(TestLevelRangeFilterBuilderProvider.class)
    public void testMinMaxLevelNull(final TestLevelRangeFilterBuilder builder) throws Exception {
        final LevelRangeFilter levelRangeFilter = builder.build(null, null, true);

        assertResult(Result.ACCEPT, levelRangeFilter, Level.ALL);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.DEBUG);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.INFO);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.WARN);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.ERROR);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.FATAL);
        assertResult(Result.ACCEPT, levelRangeFilter, Level.OFF);
    }

    private static void assertResult(final Result expected, final LevelRangeFilter filter, final Level level) {
        assertSame(expected, filter.filter(null, level, null, (Object) null, null));
    }

    private static class TestLevelRangeFilterBuilderProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(new TestLevelRangeFilterFromXmlBuilder()),
                    Arguments.of(new TestLevelRangeFilterFromPropertyBuilder()));
        }
    }

    private interface TestLevelRangeFilterBuilder {

        LevelRangeFilter build(Level levelMin, Level levelMax, Boolean acceptOnMatch) throws Exception;
    }

    private static class TestLevelRangeFilterFromXmlBuilder implements TestLevelRangeFilterBuilder {

        @Override
        public LevelRangeFilter build(final Level levelMin, final Level levelMax, final Boolean acceptOnMatch)
                throws Exception {
            final LevelRangeFilterBuilder builder = new LevelRangeFilterBuilder();
            final Filter filter = builder.parse(generateTestXml(levelMin, levelMax, acceptOnMatch), null);
            final org.apache.logging.log4j.core.Filter wrappedFilter = ((FilterWrapper) filter).getFilter();
            return (LevelRangeFilter) wrappedFilter;
        }

        private static Element generateTestXml(final Level levelMin, final Level levelMax, final Boolean acceptOnMatch)
                throws Exception {

            final StringBuilder sb = new StringBuilder();
            sb.append("<filter class=\"org.apache.log4j.varia.LevelRangeFilter\">\n");
            if (levelMin != null) {
                sb.append(String.format("<param name=\"LevelMin\" value=\"%s\"/>\n", levelMin));
            }
            if (levelMax != null) {
                sb.append(String.format("<param name=\"LevelMax\" value=\"%s\"/>\n", levelMax));
            }
            if (acceptOnMatch != null) {
                sb.append(String.format("<param name=\"AcceptOnMatch\" value=\"%b\"/>\n", acceptOnMatch));
            }
            sb.append("</filter>");

            return DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(sb.toString())))
                    .getDocumentElement();
        }
    }

    private static class TestLevelRangeFilterFromPropertyBuilder implements TestLevelRangeFilterBuilder {

        @Override
        public LevelRangeFilter build(final Level levelMin, final Level levelMax, final Boolean acceptOnMatch) {
            final Properties properties = new Properties();
            if (levelMin != null) {
                properties.setProperty("foobar.levelMin", levelMin.name());
            }
            if (levelMax != null) {
                properties.setProperty("foobar.levelMax", levelMax.name());
            }
            if (acceptOnMatch != null) {
                properties.setProperty("foobar.acceptOnMatch", acceptOnMatch.toString());
            }
            final LevelRangeFilterBuilder builder = new LevelRangeFilterBuilder("foobar", properties);
            final Filter filter = builder.parse(null);
            final org.apache.logging.log4j.core.Filter wrappedFilter = ((FilterWrapper) filter).getFilter();
            return (LevelRangeFilter) wrappedFilter;
        }
    }
}
