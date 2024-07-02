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
package example;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class ModifyConfiguration1 {

    // tag::wrong[]
    private static Appender createCustomAppender(Configuration configuration) {
        Layout<?> layout = PatternLayout.createDefaultLayout(configuration);
        return FileAppender.newBuilder()
                .setConfiguration(configuration)
                .setName("FILE")
                .setLayout(layout)
                .build();
    }

    public static void updateLoggingConfig() {
        LoggerContext context = LoggerContext.getContext(false); // <1>
        Configuration config = context.getConfiguration();
        LoggerConfig loggerConfig = config.getRootLogger();

        Appender oldAppender = loggerConfig.getAppenders().get("CONSOLE"); // <2>
        if (oldAppender != null) {
            oldAppender.stop(); // <3>
        }
        loggerConfig.removeAppender("CONSOLE");

        Appender newAppender = createCustomAppender(config); // <2>
        loggerConfig.addAppender(newAppender, null, null);
        newAppender.start(); // <4>
    }
    // end::wrong[]
}
