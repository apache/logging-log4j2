package org.apache.logging.log4j.redis.appender;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import redis.clients.jedis.JedisPoolConfig;

@Plugin(name = "PoolConfiguration", category = Core.CATEGORY_NAME, printObject = true)
public class LoggingJedisPoolConfiguration extends JedisPoolConfig {

    private LoggingJedisPoolConfiguration() {
        super();
    }

    public static LoggingJedisPoolConfiguration defaultConfiguration() {
        return new LoggingJedisPoolConfiguration();
    }

    /**
     * Creates a LoggingJedisPoolConfiguration from standard pool parameters.
     */
    @PluginFactory
    public static LoggingJedisPoolConfiguration createJedisPoolConfiguration(
            @PluginAttribute(value = "minIdle", defaultInt = 1800000) int minIdle,
            @PluginAttribute(value = "maxIdle", defaultInt = JedisPoolConfig.DEFAULT_MAX_IDLE) int maxIdle,
            @PluginAttribute(value = "testOnBorrow") boolean testOnBorrow,
            @PluginAttribute(value = "testOnReturn") boolean testOnReturn,
            @PluginAttribute(value = "testWhileIdle") boolean testWhileIdle,
            @PluginAttribute(value = "testsPerEvictionRun", defaultInt = JedisPoolConfig.DEFAULT_NUM_TESTS_PER_EVICTION_RUN) int testsPerEvictionRun,
            @PluginAttribute(value = "timeBetweenEvictionRunsMillis", defaultLong = JedisPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS) long timeBetweenEvicationRunsMillis
            ) {
        LoggingJedisPoolConfiguration poolConfig = defaultConfiguration();
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestOnBorrow(testOnBorrow);
        poolConfig.setTestOnReturn(testOnReturn);
        poolConfig.setTestWhileIdle(testWhileIdle);
        poolConfig.setNumTestsPerEvictionRun(testsPerEvictionRun);
        poolConfig.setTimeBetweenEvictionRunsMillis(timeBetweenEvicationRunsMillis);
        return poolConfig;
    }
}
