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
package org.apache.logging.log4j;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Timer;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

/**
 *
 */
public class LocationPerfCheck {

    private static final int LOOP_COUNT = 100000;


    @Test
    public void testLocation() {
        final Timer timer = new Timer("LogEvent", LOOP_COUNT);
        timer.start();
        for (int i = 0; i < LOOP_COUNT; ++i) {
            final LogEvent event1 = new Log4jLogEvent(this.getClass().getName(), null,
                "org.apache.logging.log4j.core.impl.Log4jLogEvent",
                Level.INFO, new SimpleMessage("Hello, world!"), null);
            final StackTraceElement element = event1.getSource();
        }
        timer.stop();
        System.out.println(timer.toString());
    }
}
