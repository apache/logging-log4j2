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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TagLevelTest {

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {DebugTag.class, Level.DEBUG},
            {ErrorTag.class, Level.ERROR},
            {FatalTag.class, Level.FATAL},
            {InfoTag.class, Level.INFO},
            {TraceTag.class, Level.TRACE},
            {WarnTag.class, Level.WARN}
        });
    }

    @MethodSource("data")
    @ParameterizedTest
    void testGetLevel(final Class<? extends LoggingMessageTagSupport> cls, final Level level) throws Exception {
        assertEquals(level, cls.newInstance().getLevel());
    }
}
