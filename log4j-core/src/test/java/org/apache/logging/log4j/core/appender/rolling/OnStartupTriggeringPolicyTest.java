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
package org.apache.logging.log4j.core.appender.rolling;

import java.io.ByteArrayOutputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class OnStartupTriggeringPolicyTest {

    @Test
    public void testPolicy() {
        OnStartupTriggeringPolicy policy = OnStartupTriggeringPolicy.createPolicy();
        final MyRollingManager manager = new MyRollingManager(policy, null);
        manager.setFileTime(System.currentTimeMillis() - 36000000);
        final LogEvent event = Log4jLogEvent.newBuilder() //
                .setLevel(Level.ERROR) //
                .setMessage(new SimpleMessage("Test")).build();
        assertTrue("Expected trigger to succeed", policy.isTriggeringEvent(event));
        assertTrue("Expected trigger not to fire", !policy.isTriggeringEvent(event));
        policy = OnStartupTriggeringPolicy.createPolicy();
        policy.initialize(manager);
        manager.setFileTime(System.currentTimeMillis());
        assertTrue("Expected trigger not to fire", !policy.isTriggeringEvent(event));

    }

    private class MyRollingManager extends RollingFileManager {

        private long timestamp;

        public MyRollingManager(final TriggeringPolicy policy, final RolloverStrategy strategy) {
            super("testfile", "target/rolling1/test1-%i.log.gz", new ByteArrayOutputStream(),
                false, 0, System.currentTimeMillis(), policy, strategy, null, null, 8192, true);
        }

        public void setFileTime(final long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public long getFileTime() {
            return timestamp;
        }
    }
}
