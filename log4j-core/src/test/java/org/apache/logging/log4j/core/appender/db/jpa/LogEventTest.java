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

package org.apache.logging.log4j.core.appender.db.jpa;

import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;
import org.junit.Assert;
import org.junit.Test;

public class LogEventTest {

    @Test
    public void testToImmutable_AbstractLogEventWrapperEntity() {
        final LogEvent logEvent = new AbstractLogEventWrapperEntity() {

            private static final long serialVersionUID = 1L;

            @Override
            public Map<String, String> getContextMap() {
                return null;
            }

            @Override
            public ContextStack getContextStack() {
                return null;
            }

            @Override
            public Level getLevel() {
                return null;
            }

            @Override
            public String getLoggerFqcn() {
                return null;
            }

            @Override
            public String getLoggerName() {
                return null;
            }

            @Override
            public Marker getMarker() {
                return null;
            }

            @Override
            public Message getMessage() {
                return null;
            }

            @Override
            public long getNanoTime() {
                return 0;
            }

            @Override
            public StackTraceElement getSource() {
                return null;
            }

            @Override
            public long getThreadId() {
                return 0;
            }

            @Override
            public String getThreadName() {
                return null;
            }

            @Override
            public int getThreadPriority() {
                return 0;
            }

            @Override
            public Throwable getThrown() {
                return null;
            }

            @Override
            public ThrowableProxy getThrownProxy() {
                return null;
            }

            @Override
            public long getTimeMillis() {
                return 0;
            }
        };
        Assert.assertNotSame(logEvent, logEvent.toImmutable());
    }

    @Test
    public void testToImmutable_TestBaseEntity() {
        final LogEvent logEvent = new TestBaseEntity();
        Assert.assertNotSame(logEvent, logEvent.toImmutable());
    }
}
