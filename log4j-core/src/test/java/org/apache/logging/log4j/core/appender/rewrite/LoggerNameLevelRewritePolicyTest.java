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
package org.apache.logging.log4j.core.appender.rewrite;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link LoggerNameLevelRewritePolicy}.
 * 
 * @since 2.4
 */
public class LoggerNameLevelRewritePolicyTest {

    @Test
    public void testUpdate() {
        KeyValuePair[] rewrite = new KeyValuePair[] {
                new KeyValuePair("INFO", "DEBUG"),
                new KeyValuePair("WARN", "INFO") };
        final String loggerNameRewrite = "com.foo.bar";
        LogEvent logEvent = new Log4jLogEvent(loggerNameRewrite, null, "LoggerNameLevelRewritePolicyTest.testUpdate()",
                Level.INFO, new SimpleMessage("Test"), new RuntimeException("test"), null, null, "none", null, 1);
        final LoggerNameLevelRewritePolicy updatePolicy = LoggerNameLevelRewritePolicy.createPolicy(loggerNameRewrite,
                rewrite);
        LogEvent rewritten = updatePolicy.rewrite(logEvent);
        Assert.assertEquals(Level.DEBUG, rewritten.getLevel());
        logEvent = new Log4jLogEvent(loggerNameRewrite, null, "LoggerNameLevelRewritePolicyTest.testUpdate()",
                Level.WARN, new SimpleMessage("Test"), new RuntimeException("test"), null, null, "none", null, 1);
        rewritten = updatePolicy.rewrite(logEvent);
        Assert.assertEquals(Level.INFO, rewritten.getLevel());
        final String loggerNameReadOnly = "com.nochange";
        logEvent = new Log4jLogEvent(loggerNameReadOnly, null, "LoggerNameLevelRewritePolicyTest.testUpdate()",
                Level.INFO, new SimpleMessage("Test"), new RuntimeException("test"), null, null, "none", null, 1);
        rewritten = updatePolicy.rewrite(logEvent);
        Assert.assertEquals(Level.INFO, rewritten.getLevel());
        logEvent = new Log4jLogEvent(loggerNameReadOnly, null, "LoggerNameLevelRewritePolicyTest.testUpdate()",
                Level.WARN, new SimpleMessage("Test"), new RuntimeException("test"), null, null, "none", null, 1);
        rewritten = updatePolicy.rewrite(logEvent);
        Assert.assertEquals(Level.WARN, rewritten.getLevel());
    }

}
