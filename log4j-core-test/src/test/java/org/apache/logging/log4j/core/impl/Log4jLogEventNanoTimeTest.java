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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.time.SystemNanoClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Singleton;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class Log4jLogEventNanoTimeTest {

    @LoggerContextSource("NanoTimeToFileTest.xml")
    static class DefaultTest {
        @Test
        void usesActualTimeByDefault(final LoggerContext context, @Named final ListAppender list) {
            final Logger log = context.getLogger("com.foo.Bar");
            final long before = System.nanoTime();
            log.info("Use actual System.nanoTime()");
            final Configuration configuration = context.getConfiguration();
            assertThat(configuration.getNanoClock()).isInstanceOf(SystemNanoClock.class);
            final List<String> messages = list.getMessages();
            assertThat(messages).hasSize(1);
            final String line = messages.get(0);
            final String[] parts = line.split(" AND ");
            assertThat(parts[0]).isEqualTo(parts[1]);
            assertThat(parts[2]).isEqualTo("Use actual System.nanoTime()");
            final long loggedNanoTime = Long.parseLong(parts[0]);
            assertThat(loggedNanoTime - before).isLessThan(TimeUnit.SECONDS.toNanos(1));
        }
    }

    @LoggerContextSource("NanoTimeToFileTest.xml")
    static class OverrideTest {
        private static final long DUMMYNANOTIME = 123;

        @Singleton
        @Factory
        NanoClock nanoClock() {
            return new DummyNanoClock(DUMMYNANOTIME);
        }

        @Test
        void useOverriddenTime(final LoggerContext context, @Named final ListAppender list) {
            final Logger log = context.getLogger("com.foo.Bar");
            final Configuration configuration = context.getConfiguration();
            log.info("Use dummy nano clock");
            assertThat(configuration.getNanoClock()).isInstanceOf(DummyNanoClock.class);
            final List<String> messages = list.getMessages();
            assertThat(messages).hasSize(1);
            final String line = messages.get(0);
            final String[] parts = line.split(" AND ");
            assertThat(parts[0]).isEqualTo(parts[1]).isEqualTo(String.valueOf(DUMMYNANOTIME));
            assertThat(parts[2]).isEqualTo("Use dummy nano clock");

        }
    }
}
