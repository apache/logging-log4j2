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
package org.apache.logging.slf4j;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests logging during shutdown.
 */
public class Log4j1222Test {

    @Test
    public void homepageRendersSuccessfully() {
        System.setProperty("log4j.configurationFile", "log4j2-console.xml");
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    private static class ShutdownHook extends Thread {

        private static class Holder {
            private static final Logger LOGGER = LoggerFactory.getLogger(Log4j1222Test.class);
        }

        @Override
        public void run() {
            super.run();
            trigger();
        }

        private void trigger() {
            Holder.LOGGER.info("Attempt to trigger");
            assertInstanceOf(
                    Log4jLogger.class,
                    Holder.LOGGER,
                    "Logger is of type " + Holder.LOGGER.getClass().getName());
        }
    }
}
