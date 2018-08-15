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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class RedisManagerTest {

    private JedisPool mockJedisPool;
    private Jedis mockJedis;
    private RedisManager manager;

    private String KEYS = "abc,def";

    @Before
    public void setUp() {
        initMocks();
        manager = new ManagerTestRedisAppenderBuilder()
                .setHost("localhost")
                .setPort(6379)
                .setKeys(KEYS)
                .getRedisManager();
        manager.startup();
    }

    private void initMocks() {
        mockJedisPool = Mockito.mock(JedisPool.class);
        mockJedis = Mockito.mock(Jedis.class);
        when(mockJedisPool.getResource()).thenReturn(mockJedis);
        when(mockJedis.rpush(anyString(), anyString())).thenReturn(1L);
    }

    @Test
    public void testSendsValuesToAllKeys() {
        manager.send("value");
        Mockito.verify(mockJedisPool).getResource();
        Mockito.verify(mockJedis, Mockito.times(KEYS.split(",").length)).rpush(anyString(), anyString());
        for (String k: KEYS.split(",")) {
            Mockito.verify(mockJedis, Mockito.times(1)).rpush(eq(k), anyString());
        }
    }

    @Test
    public void testReleasePoolResources() {
        manager.stop(100, TimeUnit.HOURS);
        Mockito.verify(mockJedisPool).destroy();
    }

    @Test
    public void testSendsAllValuesInBulk() {
        List<String> logs = new ArrayList<>();
        logs.add("value1");
        logs.add("value2");
        manager.sendBulk(logs);
        Mockito.verify(mockJedis, Mockito.times(KEYS.split(",").length)).rpush(anyString(), eq("value1"), eq("value2"));
    }

    private class TestRedisManager extends RedisManager {

        TestRedisManager(LoggerContext loggerContext, String name, String keys, String host, int port) {
            super(loggerContext, name, keys.split(","), host, port, null, null);
        }

        @Override
        JedisPool createPool(String host, int port, SslConfiguration ssl) {
            return mockJedisPool;
        }
    }

    private class ManagerTestRedisAppenderBuilder extends RedisAppender.Builder<ManagerTestRedisAppenderBuilder> {
        @Override
        RedisManager getRedisManager() {
            return new TestRedisManager(
                    LoggerContext.getContext(),
                    getName(),
                    getKeys(),
                    getHost(),
                    getPort()
            );
        }
    }
}
