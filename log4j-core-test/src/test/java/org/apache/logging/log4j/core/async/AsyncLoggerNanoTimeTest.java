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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.time.SystemNanoClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.plugins.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag("async")
@ContextSelectorType(AsyncLoggerContextSelector.class)
@LoggerContextSource("NanoTimeToFileTest.xml")
public class AsyncLoggerNanoTimeTest {

    @Test
    public void testAsyncLogUsesNanoTimeClock(final LoggerContext context, @Named final ListAppender list)
            throws InterruptedException {
        final AsyncLogger log = (AsyncLogger) context.getLogger("com.foo.Bar");
        final long before = System.nanoTime();
        log.info("Use actual System.nanoTime()");
        assertTrue(log.getNanoClock() instanceof SystemNanoClock, "using SystemNanoClock");

        final long DUMMYNANOTIME = -53;
        log.getContext().getConfiguration().setNanoClock(new DummyNanoClock(DUMMYNANOTIME));
        log.updateConfiguration(log.getContext().getConfiguration());

        // trigger a new nano clock lookup
        log.updateConfiguration(log.getContext().getConfiguration());

        log.info("Use dummy nano clock");
        assertTrue(log.getNanoClock() instanceof DummyNanoClock, "using SystemNanoClock");

        final List<String> messages = list.getMessages(2, 1, TimeUnit.SECONDS);
        assertThat(messages).hasSize(2);
        final String line1 = messages.get(0);
        final String line2 = messages.get(1);

        assertNotNull(line1, "line1");
        assertNotNull(line2, "line2");
        final String[] line1Parts = line1.split(" AND ");
        assertEquals("Use actual System.nanoTime()", line1Parts[2]);
        assertEquals(line1Parts[0], line1Parts[1]);
        final long loggedNanoTime = Long.parseLong(line1Parts[0]);
        assertTrue(loggedNanoTime - before < TimeUnit.SECONDS.toNanos(1), "used system nano time");

        final String[] line2Parts = line2.split(" AND ");
        assertEquals("Use dummy nano clock", line2Parts[2]);
        assertEquals(String.valueOf(DUMMYNANOTIME), line2Parts[0]);
        assertEquals(String.valueOf(DUMMYNANOTIME), line2Parts[1]);
    }

}
