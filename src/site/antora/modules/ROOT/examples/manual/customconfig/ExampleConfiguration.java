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
package com.example;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class ExampleConfiguration extends XmlConfiguration {

    public ExampleConfiguration(LoggerContext loggerContext, ConfigurationSource configSource) {
        super(loggerContext, configSource);
    }

    @Override
    protected void doConfigure() {
        Appender appender = createAppender("ExampleAppender");
        appender.start();
        addAppender(appender);
        LoggerConfig loggerConfig = LoggerConfig.newBuilder()
                .withConfig(this)
                .withAdditivity(false)
                .withLevel(Level.INFO)
                .withLoggerName("com.example")
                .withRefs(new AppenderRef[] {AppenderRef.createAppenderRef("ExampleAppender", null, null)})
                .build();
        loggerConfig.addAppender(appender, null, null);
        addLogger("com.example", loggerConfig);
    }
}
