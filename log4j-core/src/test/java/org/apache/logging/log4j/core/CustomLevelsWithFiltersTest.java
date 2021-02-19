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
package org.apache.logging.log4j.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j-customLevelsWithFilters.xml")
public class CustomLevelsWithFiltersTest {

    private Level infom1Level;
    private Level infop1Level;

    @BeforeEach
    public void before() {
        infom1Level = Level.getLevel("INFOM1");
        infop1Level = Level.getLevel("INFOP1");
    }

    @Test
    public void testConfiguration(final Configuration configuration, @Named("info") final FileAppender appender) {
        assertThat(configuration).isNotNull();
        assertThat(appender).isNotNull();
        final CompositeFilter compFilter = (CompositeFilter) appender.getFilter();
        assertThat(compFilter).isNotNull();
        final Filter[] filters = compFilter.getFiltersArray();
        assertThat(filters).isNotNull();
        boolean foundLevel = false;
        for (final Filter filter : filters) {
            final ThresholdFilter tFilter = (ThresholdFilter) filter;
            if (infom1Level.equals(tFilter.getLevel())) {
                foundLevel = true;
                break;
            }
        }
        assertTrue(foundLevel, "Level not found: " + infom1Level);
    }

    @Test
    public void testCustomLevelInts() {
        assertThat(infom1Level.intLevel()).isEqualTo(399);
        assertThat(infop1Level.intLevel()).isEqualTo(401);
    }

    @Test
    public void testCustomLevelPresence() {
        assertThat(infom1Level).isNotNull();
        assertThat(infop1Level).isNotNull();
    }

}
