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
package org.apache.logging.log4j.taglib;

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;
import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TagUtilsLevelTest {

    static Stream<Arguments> data() {
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final Level level : Level.values()) {
            builder.add(Arguments.of(level, toRootLowerCase(level.name())));
        }
        return builder.build();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testResolveLevelName(final Level level, final String levelName) throws Exception {
        assertEquals(level, TagUtils.resolveLevel(levelName));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testResolveLevelEnum(final Level level) throws Exception {
        assertEquals(level, TagUtils.resolveLevel(level));
    }
}
