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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.AbstractLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class Logger extends AbstractLogger {
    private static String FQCN = Logger.class.getName();
    private final String name;

    private final LoggerContext context;

    /**
     * config should be consistent across threads.
     */
    private volatile PrivateConfig config;

    protected Logger(LoggerContext context, String name) {
        this.context = context;
        this.name = name;
        config = new PrivateConfig(context.getConfiguration(), this);
    }

    public String getName() {
        return name;
    }

    /* @Override
    protected String getFQCN() {
        return FQCN;
    } */

    public LoggerContext getContext() {
        return context;
    }

    public synchronized void setLevel(Level level) {
        config.level = level;
        config.intLevel = level.intLevel();
    }

    public Level getLevel() {
        return config.level;
    }

    @Override
    protected void log(Marker marker, String fqcn, Level level, Message data, Throwable t) {
        config.loggerConfig.log(name, marker, fqcn, level, data, t);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg) {
        return config.filter(level, marker, msg);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Throwable t) {
        return config.filter(level, marker, msg, t);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1) {
        return config.filter(level, marker, msg, p1);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1, Object p2) {
        return config.filter(level, marker, msg, p1, p2);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1, Object p2, Object p3) {
        return config.filter(level, marker, msg, p1, p2, p3);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, String msg, Object p1, Object p2, Object p3,
                                Object... params) {
        return config.filter(level, marker, msg, p1, p2, p3, params);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, Object msg, Throwable t) {
        return config.filter(level, marker, msg, t);
    }

    @Override
    protected boolean isEnabled(Level level, Marker marker, Message msg, Throwable t) {
        return config.filter(level, marker, msg, t);
    }

    public void addAppender(Appender appender) {
        config.config.addLoggerAppender(name, appender);
    }

    public void removeAppender(Appender appender) {
        config.loggerConfig.removeAppender(appender.getName());
    }

    public Map<String, Appender> getAppenders() {
         return config.loggerConfig.getAppenders();
    }

    public List<Filter> getFilters() {
        return config.loggerConfig.getFilters();
    }

    /**
     * This method isn't synchronized to serialized updates to config. Rather, by doing this
     * it is guaranteed that all threads will see the update without having to declare the variable
     * volatile.
     *
     * @param config The new Configuration.
     */
    void updateConfiguration(Configuration config) {
        this.config = new PrivateConfig(config, this);
    }

    private class PrivateConfig {
        private final LoggerConfig loggerConfig;
        private final Configuration config;
        private Level level;
        private int intLevel;
        private final Logger logger;

        public PrivateConfig(Configuration config, Logger logger) {
            this.config = config;
            this.loggerConfig = config.getLoggerConfig(name);
            this.level = this.loggerConfig.getLevel();
            this.intLevel = this.level.intLevel();
            this.logger = logger;
        }

        public PrivateConfig(PrivateConfig pc, LoggerConfig lc) {
            this.config = pc.config;
            this.loggerConfig = lc;
            this.level = lc.getLevel();
            this.intLevel = this.level.intLevel();
            this.logger = pc.logger;
        }

        boolean filter(Level level, Marker marker, String msg) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return level.lessOrEqual(intLevel);
        }

        boolean filter(Level level, Marker marker, String msg, Throwable t) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, t);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return level.lessOrEqual(intLevel);
        }

        boolean filter(Level level, Marker marker, String msg, Object p1) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, p1);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return level.lessOrEqual(intLevel);
        }

        boolean filter(Level level, Marker marker, String msg, Object p1, Object p2) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, p1, p2);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return level.lessOrEqual(intLevel);
        }

        boolean filter(Level level, Marker marker, String msg, Object p1, Object p2, Object p3) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, p1, p2, p3);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return level.lessOrEqual(intLevel);
        }

        boolean filter(Level level, Marker marker, String msg, Object p1, Object p2, Object p3,
                       Object... params) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, p1, p2, p3, params);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return level.lessOrEqual(intLevel);
        }

        boolean filter(Level level, Marker marker, Object msg, Throwable t) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, t);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return level.lessOrEqual(intLevel);
        }

        boolean filter(Level level, Marker marker, Message msg, Throwable t) {
            if (config.hasFilters()) {
                Iterator<Filter> iter = config.getFilters();
                while (iter.hasNext()) {
                    Filter filter = iter.next();
                    Filter.Result r = filter.filter(logger, level, marker, msg, t);
                    if (r != Filter.Result.NEUTRAL) {
                        return r == Filter.Result.ACCEPT;
                    }
                }
            }

            return level.lessOrEqual(intLevel);
        }
    }
}
