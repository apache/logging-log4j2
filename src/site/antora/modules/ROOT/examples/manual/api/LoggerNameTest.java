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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringMapMessage;

// tag::class[]
public class LoggerNameTest {

    Logger logger1 = LogManager.getLogger(LoggerNameTest.class);

    Logger logger2 = LogManager.getLogger(LoggerNameTest.class.getName());

    Logger logger3 = LogManager.getLogger();
    // tag::examples[]

    private static final Logger LOGGER = LogManager.getLogger();

    static void logExamples() {
        // tag::example1[]
        LOGGER.info("Hello, {}!", name);
        // end::example1[]
        // tag::example2[]
        LOGGER.log(Level.INFO, messageFactory.createMessage("Hello, {}!", new Object[] {name}));
        // end::example2[]
        // tag::example3[]
        LogManager.getLogger() // <1>
                .info("Hello, {}!", name); // <2>

        LogManager.getLogger(StringFormatterMessageFactory.INSTANCE) // <3>
                .info("Hello, %s!", name); // <4>
        // end::example3[]
        // tag::formatter[]
        Logger logger = LogManager.getFormatterLogger();
        logger.debug("Logging in user %s with birthday %s", user.getName(), user.getBirthdayCalendar());
        logger.debug(
                "Logging in user %1$s with birthday %2$tm %2$te,%2$tY", user.getName(), user.getBirthdayCalendar());
        logger.debug("Integer.MAX_VALUE = %,d", Integer.MAX_VALUE);
        logger.debug("Long.MAX_VALUE = %,d", Long.MAX_VALUE);
        // end::formatter[]
        // tag::printf[]
        Logger logger = LogManager.getLogger("Foo");
        logger.debug("Opening connection to {}...", someDataSource);
        logger.printf(Level.INFO, "Hello, %s!", userName);
        // end::printf[]
        // tag::fluent[]
        LOGGER.atInfo()
                .withMarker(marker)
                .withLocation()
                .withThrowable(exception)
                .log("Login for user `{}` failed", userId);
        // end::fluent[]
        // tag::string[]
        LOGGER.info("foo");
        LOGGER.info(new SimpleMessage("foo"));
        // end::string[]
        // tag::parameterized[]
        LOGGER.info("foo {} {}", "bar", "baz");
        LOGGER.info(new ParameterizedMessage("foo {} {}", new Object[] {"bar", "baz"}));
        // end::parameterized[]
        // tag::map[]
        LOGGER.info(new StringMapMessage().with("key1", "val1").with("key2", "val2"));
        // end::map[]
    }

    static void contextMap() {
        // tag::thread-context1[]
        ThreadContext.put("ipAddress", request.getRemoteAddr()); // <1>
        ThreadContext.put("hostName", request.getServerName()); // <1>
        ThreadContext.put("loginId", session.getAttribute("loginId")); // <1>
        // end::thread-context1[]
    }

    // tag::thread-context2[]
    void performWork() {
        ThreadContext.push("performWork()"); // <2>

        LOGGER.debug("Performing work"); // <3>
        // Perform the work

        ThreadContext.pop(); // <4>
    }
    // end::thread-context2[]

    static void clean() {
        // tag::thread-context3[]
        ThreadContext.clear(); // <5>
        // end::thread-context3[]
    }
    // end::examples[]
}
// end::class[]
