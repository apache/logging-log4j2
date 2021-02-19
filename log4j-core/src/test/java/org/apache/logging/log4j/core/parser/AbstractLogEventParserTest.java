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
package org.apache.logging.log4j.core.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Subclassed by JSON, XML, and YAML modules.
 */
public abstract class AbstractLogEventParserTest {

    protected void assertLogEvent(final LogEvent logEvent) {
        assertThat(logEvent).isNotNull();
        assertThat(logEvent.getInstant().getEpochMillisecond()).isEqualTo(1493121664118L);
        assertThat(logEvent.getThreadName()).isEqualTo("main");
        assertThat(logEvent.getThreadId()).isEqualTo(1L);
        assertThat(logEvent.getThreadPriority()).isEqualTo(5);
        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getLoggerName()).isEqualTo("HelloWorld");
        assertThat(logEvent.getMarker().getName()).isEqualTo("child");
        assertThat(logEvent.getMarker().getParents()[0].getName()).isEqualTo("parent");
        assertThat(logEvent.getMarker().getParents()[0].getParents()[0].getName()).isEqualTo("grandparent");
        assertThat(logEvent.getMessage().getFormattedMessage()).isEqualTo("Hello, world!");
        assertThat(logEvent.getThrown()).isNull();
        assertThat(logEvent.getThrownProxy().getMessage()).isEqualTo("error message");
        assertThat(logEvent.getThrownProxy().getName()).isEqualTo("java.lang.RuntimeException");
        assertThat(logEvent.getThrownProxy().getExtendedStackTrace()[0].getClassName()).isEqualTo("logtest.Main");
        assertThat(logEvent.getLoggerFqcn()).isEqualTo("org.apache.logging.log4j.spi.AbstractLogger");
        assertThat(logEvent.getContextStack().asList()).isEqualTo(Arrays.asList("one", "two"));
        assertThat(logEvent.getContextData().<String>getValue("foo")).isEqualTo("FOO");
        assertThat(logEvent.getContextData().<String>getValue("bar")).isEqualTo("BAR");
        assertThat(logEvent.getSource().getClassName()).isEqualTo("logtest.Main");
    }
}
