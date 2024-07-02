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

import java.net.URI;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class ModifyConfiguration2 {

    private static Appender createCustomAppender(Configuration configuration) {
        Layout<?> layout = PatternLayout.createDefaultLayout(configuration);
        return FileAppender.newBuilder()
                .setConfiguration(configuration)
                .setName("FILE")
                .setLayout(layout)
                .build();
    }

    // tag::right[]
    public static void updateLoggingConfig() {
        URI location = URI.create("classpath:log4j2.xml");
        Configurator.reconfigure(new CustomConfiguration(location)); // <1>
    }

    private static class CustomConfiguration extends XmlConfiguration {
        public CustomConfiguration(URI configLocation) {
            super(null, ConfigurationSource.fromUri(configLocation));
        }

        @Override
        protected void doConfigure() {
            super.doConfigure();
            LoggerConfig loggerConfig = getRootLogger();

            loggerConfig.removeAppender("CONSOLE"); // <2> <3>

            Appender newAppender = createCustomAppender(this);
            loggerConfig.addAppender(newAppender, null, null); // <4>
        }
    }
    // end::right[]
}
