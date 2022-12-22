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
package org.apache.logging.log4j.instrument.log4j2;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;

import static org.assertj.core.api.Assertions.assertThat;

public class LogBuilderConversionHandlerExample {

    private static final Logger logger = LogManager.getLogger();

    private static final int referenceLine = 32;

    public void testWithLocation(final ListAppender app) {
        app.clear();
        logger.atInfo().withLocation().log();
        assertLocationEquals("testWithLocation", referenceLine + 4, app);
        logger.atInfo()
                .withLocation(new StackTraceElement(LogBuilderConversionHandlerExample.class.getName(), "specialMethod",
                        "LogBuilderConversionHandlerExample.java", 1024))
                .log();
        assertLocationEquals("specialMethod", 1024, app);
    }

    private static void assertLocationEquals(final String methodName, final int lineNumber, final ListAppender app) {
        final List<LogEvent> events = app.getEvents();
        assertThat(events).hasSize(1);
        final LogEvent event = events.get(0);
        assertThat(event.isIncludeLocation()).isFalse();
        assertThat(event.getSource()).isNotNull();
        final StackTraceElement location = event.getSource();
        assertThat(location.getClassName()).isEqualTo(LogBuilderConversionHandlerExample.class.getName());
        assertThat(location.getMethodName()).isEqualTo(methodName);
        assertThat(location.getFileName()).isEqualTo("LogBuilderConversionHandlerExample.java");
        assertThat(location.getLineNumber()).isEqualTo(lineNumber);
        app.clear();
    }
}
