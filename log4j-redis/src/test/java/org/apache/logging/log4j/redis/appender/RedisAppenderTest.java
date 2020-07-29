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

package org.apache.logging.log4j.redis.appender;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Tests {@link RedisAppender}.
 */
public final class RedisAppenderTest {

    private RedisAppender appender;
    private Log4jLogEvent logEvent;
    private RedisManager manager;

    private String DESTINATION_KEY = "destination";
    private String HOST = "localhost";
    private int PORT = 6379;
    private String MESSAGE = "Important Message";

    @Before
    public void setUp() {
        initMocks();

        appender = new AppenderTestRedisAppenderBuilder()
                        .setName("RedisAppender")
                        .setKeys(DESTINATION_KEY)
                        .setHost(HOST)
                        .setPort(PORT)
                        .setImmediateFlush(true)
                        .setLayout(PatternLayout.createDefaultLayout())
                        .build();
        logEvent = createLogEvent();
    }

    private void initMocks() {
        manager = Mockito.mock(RedisManager.class);
        when(manager.createPool(HOST, PORT, null)).thenReturn(Mockito.mock(JedisPool.class));
    }

    @Test
    public void testAppenderStartsProperly() {
        appender.start();
        Mockito.verify(manager, Mockito.times(1)).startup();
        assertEquals(appender.getState(), LifeCycle.State.STARTED);
    }

    @Test
    public void testAppenderStopsProperly() {
        appender.stop(500, TimeUnit.HOURS);
        Mockito.verify(manager, Mockito.times(1)).stop(500, TimeUnit.HOURS);
        assertEquals(appender.getState(), LifeCycle.State.STOPPED);
    }

    @Test
    public void testAppendLogEvent() {
        appender.append(logEvent);
        Mockito.verify(manager, Mockito.times(1)).sendBulk(any());
    }

    @Test
    public void testQueuesLogEvents() {
        appender = new AppenderTestRedisAppenderBuilder()
                .setName("RedisAppender")
                .setKeys(DESTINATION_KEY)
                .setHost(HOST)
                .setPort(PORT)
                .setQueueCapacity(2)
                .setImmediateFlush(false)
                .setLayout(PatternLayout.newBuilder().setPattern("%m").build())
                .build();

        appender.append(logEvent);
        Mockito.verify(manager, Mockito.times(0)).sendBulk(any());
    }

    @Test
    public void testAttemptsSendWhenQueueReachesCapacity() {
        appender = new AppenderTestRedisAppenderBuilder()
                .setName("RedisAppender")
                .setKeys(DESTINATION_KEY)
                .setHost(HOST)
                .setPort(PORT)
                .setQueueCapacity(1)
                .setImmediateFlush(false)
                .setLayout(PatternLayout.newBuilder().setPattern("%m").build())
                .build();

        appender.append(logEvent);
        appender.append(logEvent);
        appender.append(logEvent);
        Mockito.verify(manager, Mockito.times(2)).sendBulk(anyList());
    }

    @Test
    public void testFlushesQueueAtExceededCapacity() {
        appender = new AppenderTestRedisAppenderBuilder()
                .setName("RedisAppender")
                .setKeys(DESTINATION_KEY)
                .setHost(HOST)
                .setPort(PORT)
                .setQueueCapacity(1)
                .setImmediateFlush(false)
                .setLayout(PatternLayout.newBuilder().setPattern("%m").build())
                .build();

        appender.append(logEvent);
        appender.append(logEvent);
        Mockito.verify(manager, Mockito.times(1)).sendBulk(any());
    }

    @Test
    public void testFlushesQueueAtEndOfBatch() {
        appender = new AppenderTestRedisAppenderBuilder()
                .setName("RedisAppender")
                .setKeys(DESTINATION_KEY)
                .setHost(HOST)
                .setPort(PORT)
                .setImmediateFlush(false)
                .setLayout(PatternLayout.newBuilder().setPattern("%m").build())
                .build();
        logEvent = createPartialLogEvent().setEndOfBatch(true).build();

        appender.append(logEvent);
        Mockito.verify(manager, Mockito.times(1)).sendBulk(any());
    }

    @Test
    public void testFlushesQueueOnAppenderStop() {
        appender = new AppenderTestRedisAppenderBuilder()
                .setName("RedisAppender")
                .setKeys(DESTINATION_KEY)
                .setHost(HOST)
                .setPort(PORT)
                .setImmediateFlush(false)
                .setLayout(PatternLayout.newBuilder().setPattern("%m").build())
                .build();
        appender.append(logEvent);
        Mockito.verify(manager, Mockito.times(0)).sendBulk(any());
        appender.stop(100, TimeUnit.DAYS);
        Mockito.verify(manager, Mockito.times(1)).sendBulk(any());
    }

    private Log4jLogEvent createLogEvent() {
        return createPartialLogEvent().build();
    }

    private Log4jLogEvent.Builder createPartialLogEvent() {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(RedisAppenderTest.class.getName())
                .setLoggerFqcn(RedisAppenderTest.class.getName())
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage(MESSAGE));
    }

    private class AppenderTestRedisAppenderBuilder extends RedisAppender.Builder<AppenderTestRedisAppenderBuilder> {
        @Override
        RedisManager getRedisManager() {
            return manager;
        }
    }
}
