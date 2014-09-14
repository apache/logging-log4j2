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

import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * The default configuration writes all output to the Console using the default logging level. You configure default
 * logging level by setting the system property "org.apache.logging.log4j.level" to a level name. If you do not
 * specify the property, Log4j uses the ERROR Level. Log Events will be printed using the basic formatting provided
 * by each Message.
 */
public class DefaultConfiguration extends AbstractConfiguration {

    private static final long serialVersionUID = 1L;

    /**
     * The name of the default configuration.
     */
    public static final String DEFAULT_NAME = "Default";
    /**
     * The System Property used to specify the logging level.
     */
    public static final String DEFAULT_LEVEL = "org.apache.logging.log4j.level";
    /**
     * The default Pattern used for the default Layout.
     */
    public static final String DEFAULT_PATTERN = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";

    /**
     * Constructor to create the default configuration.
     */
    public DefaultConfiguration() {
        super(ConfigurationSource.NULL_SOURCE);
        
        setName(DEFAULT_NAME);
        final Layout<? extends Serializable> layout = PatternLayout.newBuilder()
            .withPattern(DEFAULT_PATTERN)
            .withConfiguration(this)
            .build();
        final Appender appender = ConsoleAppender.createDefaultAppenderForLayout(layout);
        appender.start();
        addAppender(appender);
        final LoggerConfig root = getRootLogger();
        root.addAppender(appender, null, null);

        final String levelName = PropertiesUtil.getProperties().getStringProperty(DEFAULT_LEVEL);
        final Level level = levelName != null && Level.valueOf(levelName) != null ?
            Level.valueOf(levelName) : Level.ERROR;
        root.setLevel(level);
    }

    @Override
    protected void doConfigure() {
    }
}
