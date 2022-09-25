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
package org.apache.logging.log4j.spring.boot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SetSystemProperty(key = "spring.profiles.active", value = "prod")
@SetSystemProperty(key = "log4j2.loggerContextFactory", value = "org.apache.logging.log4j.core.impl.Log4jContextFactory")
@SpringBootTest
public class Log4j2SpringBootInitTest {

    @Test
    public void testEnvironment() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        ListAppender app = context.getConfiguration().getAppender("Out");
        assertNotNull(app);
        assertEquals(1, app.getMessages().size());
        assertEquals("Started: log4j-spring-boot", app.getMessages().get(0));
    }

    @SpringBootApplication
    public static class SpringTestApplication implements ApplicationRunner {
        private final Logger LOGGER = LogManager.getLogger("org.apache.logging.log4j.core.springtest");
        public static void main(String[] args) {
            SpringApplication.run(SpringTestApplication.class, args);
        }

        @Override
        public void run(ApplicationArguments args) throws Exception {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            SpringLookup lookup = new SpringLookup();
            lookup.setLoggerContext(context);
            LOGGER.info("Started: {}", lookup.lookup("spring.application.name"));
        }
    }
}

