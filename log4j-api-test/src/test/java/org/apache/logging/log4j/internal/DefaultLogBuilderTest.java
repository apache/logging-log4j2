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
package org.apache.logging.log4j.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.test.TestLogger;
import org.junit.jupiter.api.Test;

public class DefaultLogBuilderTest {

    private final TestLogger logger1 = (TestLogger) LogManager.getLogger(DefaultLogBuilderTest.class);
    private final TestLogger logger2 = (TestLogger) LogManager.getLogger("second.logger");

    @Test
    public void testConcurrentUsage() {
        logger1.getEntries().clear();
        logger2.getEntries().clear();
        final List<LogBuilder> logBuilders =
                Arrays.asList(logger1.atDebug(), logger1.atInfo(), logger2.atDebug(), logger2.atInfo());
        logBuilders.forEach(logBuilder -> logBuilder.log("Hello LogBuilder!"));
        assertThat(logger1.getEntries())
                .hasSize(2)
                .containsExactly(" DEBUG Hello LogBuilder!", " INFO Hello LogBuilder!");
        assertThat(logger2.getEntries())
                .hasSize(2)
                .containsExactly(" DEBUG Hello LogBuilder!", " INFO Hello LogBuilder!");
    }
}
