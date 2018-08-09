package org.apache.logging.log4j.redis.appender;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class RedisManagerTest {

    JedisPool mockJedisPool;
    Jedis mockJedis;
    RedisManager manager;

    String[] KEYS = {"abc", "def"};
    private String HOST = "localhost";
    private int PORT = 6379;

    @Before
    public void setUp() throws Exception {
        initMocks();
        manager = new ManagerTestRedisAppenderBuilder()
                .setHost(HOST)
                .setPort(PORT)
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
        Mockito.verify(mockJedis, Mockito.times(KEYS.length)).rpush(anyString(), anyString());
        for (String k: KEYS) {
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
        manager.sendBulk(Arrays.asList("value1", "value2"));
        Mockito.verify(mockJedis, Mockito.times(KEYS.length)).rpush(anyString(), eq("value1"));
        Mockito.verify(mockJedis, Mockito.times(KEYS.length)).rpush(anyString(), eq("value2"));
    }

    private class TestRedisManager extends RedisManager {

        TestRedisManager(LoggerContext loggerContext, String name, String[] keys, String host, int port) {
            super(loggerContext, name, keys, host, port, null, null);
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
