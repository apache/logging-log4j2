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
package org.apache.log4j.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class Log4j1LevelPatternConverterTest {

    /**
     * Tests if the converter returns the Log4j 1.x {@code toString()} value of
     * custom Log4j 1.x levels.
     *
     * @param level a Log4j 1.x level
     */
    @ParameterizedTest
    @MethodSource("org.apache.log4j.helpers.UtilLoggingLevel#getAllPossibleLevels")
    public void testUtilLoggingLevels(final Level level) {
        final Log4j1LevelPatternConverter converter = Log4j1LevelPatternConverter.newInstance(null);
        final LogEvent logEvent = mock(LogEvent.class);
        when(logEvent.getLevel()).thenReturn(level.getVersion2Level());
        final StringBuilder result = new StringBuilder();
        converter.format(logEvent, result);
        assertEquals(level.toString(), result.toString());
    }
}
