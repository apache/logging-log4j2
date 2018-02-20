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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Subclassed by JSON, XML, and YAML modules.
 */
public abstract class AbstractLogEventParserTest {
    
    protected void assertLogEvent(final LogEvent logEvent) {
        assertThat(logEvent, is(notNullValue()));
        assertThat(logEvent.getInstant().getEpochMillisecond(), equalTo(1493121664118L));
        assertThat(logEvent.getThreadName(), equalTo("main"));
        assertThat(logEvent.getThreadId(), equalTo(1L));
        assertThat(logEvent.getThreadPriority(), equalTo(5));
        assertThat(logEvent.getLevel(), equalTo(Level.INFO));
        assertThat(logEvent.getLoggerName(), equalTo("HelloWorld"));
        assertThat(logEvent.getMarker().getName(), equalTo("child"));
        assertThat(logEvent.getMarker().getParents()[0].getName(), equalTo("parent"));
        assertThat(logEvent.getMarker().getParents()[0].getParents()[0].getName(),
                equalTo("grandparent"));
        assertThat(logEvent.getMessage().getFormattedMessage(), equalTo("Hello, world!"));
        assertThat(logEvent.getThrown(), is(nullValue()));
        assertThat(logEvent.getThrownProxy().getMessage(), equalTo("error message"));
        assertThat(logEvent.getThrownProxy().getName(), equalTo("java.lang.RuntimeException"));
        assertThat(logEvent.getThrownProxy().getExtendedStackTrace()[0].getClassName(),
                equalTo("logtest.Main"));
        assertThat(logEvent.getLoggerFqcn(), equalTo("org.apache.logging.log4j.spi.AbstractLogger"));
        assertThat(logEvent.getContextStack().asList(), equalTo(Arrays.asList("one", "two")));
        assertThat((String) logEvent.getContextData().getValue("foo"), equalTo("FOO"));
        assertThat((String) logEvent.getContextData().getValue("bar"), equalTo("BAR"));
        assertThat(logEvent.getSource().getClassName(), equalTo("logtest.Main"));
    }
}
