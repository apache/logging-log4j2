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

package org.apache.log4j.bridge;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class LogEventWrapperTest {

    @Test
    public void testThread() {
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        LoggingEvent log4j1Event = new LoggingEvent() {

            @Override
            public String getThreadName() {
                return threadName;
            }
        };
        LogEvent log4j2Event = new LogEventWrapper(log4j1Event);
        assertEquals(currentThread.getId(), log4j2Event.getThreadId());
        assertEquals(currentThread.getPriority(), log4j2Event.getThreadPriority());
    }

    @Test
    public void testToImmutable() {
        LogEventWrapper wrapper = new LogEventWrapper(new LoggingEvent());
        assertSame(wrapper, wrapper.toImmutable());
    }
}
