/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.simple.internal;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.simple.SimpleLoggerContext;
import org.apache.logging.log4j.simple.SimpleLoggerContextFactory;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.NoOpThreadContextMap;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Provider} implementation to use {@link SimpleLoggerContext}.
 *
 * @since 2.24.0
 */
@NullMarked
public final class SimpleProvider extends Provider {

    private final ThreadContextMap threadContextMap;

    public SimpleProvider() {
        super(null, CURRENT_VERSION);
        this.threadContextMap =
                Config.INSTANCE.showContextMap ? super.getThreadContextMapInstance() : NoOpThreadContextMap.INSTANCE;
    }

    @Override
    public LoggerContextFactory getLoggerContextFactory() {
        return SimpleLoggerContextFactory.INSTANCE;
    }

    @Override
    public ThreadContextMap getThreadContextMapInstance() {
        return threadContextMap;
    }

    public static final class Config {

        /** The default format to use when formatting dates */
        private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS zzz";

        /** All system properties used by <code>SimpleLog</code> start with this */
        private static final String SYSTEM_PREFIX = "org.apache.logging.log4j.simplelog.";

        private static final String SYSTEM_OUT = "system.out";

        private static final String SYSTEM_ERR = "system.err";

        public static final Config INSTANCE = new Config();

        public final PropertiesUtil props;

        public final boolean showContextMap;

        public final boolean showLogName;

        public final boolean showShortName;

        public final boolean showDateTime;

        public final Level defaultLevel;

        public final @Nullable String dateTimeFormat;

        public final PrintStream stream;

        private Config() {
            props = new PropertiesUtil("log4j2.simplelog.properties");

            showContextMap = props.getBooleanProperty(SYSTEM_PREFIX + "showContextMap", false);
            showLogName = props.getBooleanProperty(SYSTEM_PREFIX + "showlogname", false);
            showShortName = props.getBooleanProperty(SYSTEM_PREFIX + "showShortLogname", true);
            showDateTime = props.getBooleanProperty(SYSTEM_PREFIX + "showdatetime", false);
            final String lvl = props.getStringProperty(SYSTEM_PREFIX + "level");
            defaultLevel = Level.toLevel(lvl, Level.ERROR);

            dateTimeFormat = showDateTime
                    ? props.getStringProperty(SYSTEM_PREFIX + "dateTimeFormat", DEFAULT_DATE_TIME_FORMAT)
                    : null;

            final String fileName = props.getStringProperty(SYSTEM_PREFIX + "logFile", SYSTEM_ERR);
            PrintStream ps;
            if (SYSTEM_ERR.equalsIgnoreCase(fileName)) {
                ps = System.err;
            } else if (SYSTEM_OUT.equalsIgnoreCase(fileName)) {
                ps = System.out;
            } else {
                try {
                    ps = new PrintStream(new FileOutputStream(fileName));
                } catch (final FileNotFoundException fnfe) {
                    ps = System.err;
                }
            }
            this.stream = ps;
        }
    }
}
