/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
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

    @Before
    public void setUp() {
        initMocks();

        appender = new AppenderTestRedisAppenderBuilder()
                        .withName("RedisAppender")
                        .withKeys(DESTINATION_KEY)
                        .withHost(HOST)
                        .withPort(PORT)
                        .withLayout(PatternLayout.createDefaultLayout())
                        .build();
        logEvent = createLogEvent();
    }

    private void initMocks() {
        manager = Mockito.mock(RedisManager.class);
        when(manager.createPool(HOST, PORT, true)).thenReturn(Mockito.mock(JedisPool.class));
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
        byte[] expectedResult = appender.getLayout().toByteArray(logEvent);
        Mockito.verify(manager, Mockito.times(1)).send(expectedResult);
    }

    private Log4jLogEvent createLogEvent() {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(RedisAppenderTest.class.getName())
                .setLoggerFqcn(RedisAppenderTest.class.getName())
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("Important Message"))
                .build();
    }

    private class AppenderTestRedisAppenderBuilder extends RedisAppender.Builder<AppenderTestRedisAppenderBuilder> {
        @Override
        RedisManager getRedisManager() {
            return manager;
        }
    }
}
