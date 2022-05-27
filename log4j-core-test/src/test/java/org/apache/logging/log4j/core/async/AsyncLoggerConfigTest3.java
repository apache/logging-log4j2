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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.test.junit.CleanUpFiles;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

@Tag("async")
@Tag("sleepy")
public class AsyncLoggerConfigTest3 {

    @Test
    @CleanUpFiles("target/AsyncLoggerConfigTest2.log")
    @LoggerContextSource("AsyncLoggerConfigTest2.xml")
    public void testNoConcurrentModificationException(final Logger log) throws Exception {
        log.info("initial message");
        Thread.sleep(500);

        final Map<String, String> map = new HashMap<>();
        for (int j = 0; j < 3000; j++) {
            map.put(String.valueOf(j), String.valueOf(System.nanoTime()));
        }

        final Message msg = new ParameterizedMessage("{}", map);
        Log4jLogEvent event = Log4jLogEvent.newBuilder()
                .setLevel(Level.WARN)
                .setLoggerName(getClass().getName())
                .setMessage(msg)
                .setTimeMillis(0).build();

        for (int i = 0; i < 100; i++) {
            ((AsyncLoggerConfig)((org.apache.logging.log4j.core.Logger) log).get()).callAppenders(event);
            for (int j = 0; j < 3000; j++) {
                map.remove(String.valueOf(j));
            }
            for (int j = 0; j < 3000; j++) {
                map.put(String.valueOf(j), String.valueOf(System.nanoTime()));
            }
        }
    }
}
