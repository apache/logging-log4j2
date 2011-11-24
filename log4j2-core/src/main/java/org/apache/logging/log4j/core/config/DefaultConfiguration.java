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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * The default configuration. It writes all output to the Console using the default logging level
 * that is configured by setting the "org.apache.logging.log4j.level" system property or the ERROR
 * Level if the system property is not specified. Log Events will be printed using the basic
 * formatting provided by each Message.
 */
public class DefaultConfiguration extends BaseConfiguration {

    /**
     * The name of the default configuration.
     */
    public static final String DEFAULT_NAME = "Default";
    /**
     * The System Proerty used to specify the logging level.
     */
    public static final String DEFAULT_LEVEL = "org.apache.logging.log4j.level";

    private static final String EMPTY_STRING = "";

    /**
     * Constructor to create the default configuration.
     */
    public DefaultConfiguration() {

        setName(DEFAULT_NAME);
        Layout layout = PatternLayout.createLayout("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n",
            null, null, null);
        Appender appender = new ConsoleAppender("Console", layout);
        addAppender(appender);
        LoggerConfig root = getRootLogger();
        root.addAppender(appender);

        String l = System.getProperty(DEFAULT_LEVEL);
        Level level = (l != null && Level.valueOf(l) != null) ? Level.valueOf(l) : Level.ERROR;
        root.setLevel(level);
    }
}
