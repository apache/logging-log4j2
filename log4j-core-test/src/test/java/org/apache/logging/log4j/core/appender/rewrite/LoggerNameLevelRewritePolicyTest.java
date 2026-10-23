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
package org.apache.logging.log4j.core.appender.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link LoggerNameLevelRewritePolicy}.
 *
 * @since 2.4
 */
class LoggerNameLevelRewritePolicyTest {

    @Test
    void testUpdate() {
        final KeyValuePair[] rewrite =
                new KeyValuePair[] {new KeyValuePair("INFO", "DEBUG"), new KeyValuePair("WARN", "INFO")};
        final String loggerNameRewrite = "com.foo.bar";
        LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(loggerNameRewrite)
                .setLoggerFqcn("LoggerNameLevelRewritePolicyTest.testUpdate()")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("Test"))
                .setThrown(new RuntimeException("test"))
                .setThreadName("none")
                .setTimeMillis(1)
                .build();
        final LoggerNameLevelRewritePolicy updatePolicy =
                LoggerNameLevelRewritePolicy.createPolicy(loggerNameRewrite, rewrite);
        LogEvent rewritten = updatePolicy.rewrite(logEvent);
        assertEquals(Level.DEBUG, rewritten.getLevel());
        logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(loggerNameRewrite)
                .setLoggerFqcn("LoggerNameLevelRewritePolicyTest.testUpdate()")
                .setLevel(Level.WARN)
                .setMessage(new SimpleMessage("Test"))
                .setThrown(new RuntimeException("test"))
                .setThreadName("none")
                .setTimeMillis(1)
                .build();
        rewritten = updatePolicy.rewrite(logEvent);
        assertEquals(Level.INFO, rewritten.getLevel());
        final String loggerNameReadOnly = "com.nochange";
        logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(loggerNameReadOnly)
                .setLoggerFqcn("LoggerNameLevelRewritePolicyTest.testUpdate()")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("Test"))
                .setThrown(new RuntimeException("test"))
                .setThreadName("none")
                .setTimeMillis(1)
                .build();
        rewritten = updatePolicy.rewrite(logEvent);
        assertEquals(Level.INFO, rewritten.getLevel());
        logEvent = Log4jLogEvent.newBuilder()
                .setLoggerName(loggerNameReadOnly)
                .setLoggerFqcn("LoggerNameLevelRewritePolicyTest.testUpdate()")
                .setLevel(Level.WARN)
                .setMessage(new SimpleMessage("Test"))
                .setThrown(new RuntimeException("test"))
                .setThreadName("none")
                .setTimeMillis(1)
                .build();
        rewritten = updatePolicy.rewrite(logEvent);
        assertEquals(Level.WARN, rewritten.getLevel());
    }

    /**
     * {@code rewrite} matches the configured logger name with
     * {@link String#startsWith(String)}, so a policy configured for {@code "com.foo"} must not also match a
     * sibling package such as {@code "com.foobar"}, which merely happens to share the same character prefix
     * without being a sub-logger of it. It must still match a genuine child logger such as
     * {@code "com.foo.Bar"}, consistent with every other logger-name-hierarchy feature in Log4j.
     */
    @Test
    void testPrefixMatchRespectsLoggerHierarchyBoundary() {
        final KeyValuePair[] rewrite = new KeyValuePair[] {new KeyValuePair("WARN", "DEBUG")};
        final LoggerNameLevelRewritePolicy policy = LoggerNameLevelRewritePolicy.createPolicy("com.foo", rewrite);

        final LogEvent siblingPackageEvent = Log4jLogEvent.newBuilder()
                .setLoggerName("com.foobar.PaymentProcessor")
                .setLevel(Level.WARN)
                .setMessage(new SimpleMessage("Test"))
                .build();
        assertEquals(
                Level.WARN,
                policy.rewrite(siblingPackageEvent).getLevel(),
                "a rule for 'com.foo' must not also match the unrelated sibling package 'com.foobar'");

        final LogEvent childLoggerEvent = Log4jLogEvent.newBuilder()
                .setLoggerName("com.foo.Bar")
                .setLevel(Level.WARN)
                .setMessage(new SimpleMessage("Test"))
                .build();
        assertEquals(
                Level.DEBUG,
                policy.rewrite(childLoggerEvent).getLevel(),
                "a rule for 'com.foo' must still match its own child logger 'com.foo.Bar'");

        final LogEvent exactMatchEvent = Log4jLogEvent.newBuilder()
                .setLoggerName("com.foo")
                .setLevel(Level.WARN)
                .setMessage(new SimpleMessage("Test"))
                .build();
        assertEquals(
                Level.DEBUG,
                policy.rewrite(exactMatchEvent).getLevel(),
                "a rule for 'com.foo' must still match the exact logger name 'com.foo'");
    }

    /**
     * A configured name that already ends with the separator is itself an unambiguous prefix (the separator
     * supplies the hierarchy boundary), so it must keep matching every descendant with a plain prefix check,
     * exactly as before {@link #testPrefixMatchRespectsLoggerHierarchyBoundary}'s fix.
     */
    @Test
    void testTrailingDotPrefixStillMatchesDescendant() {
        final KeyValuePair[] rewrite = new KeyValuePair[] {new KeyValuePair("WARN", "DEBUG")};
        final LoggerNameLevelRewritePolicy policy = LoggerNameLevelRewritePolicy.createPolicy("com.foo.", rewrite);

        final LogEvent childLoggerEvent = Log4jLogEvent.newBuilder()
                .setLoggerName("com.foo.Bar")
                .setLevel(Level.WARN)
                .setMessage(new SimpleMessage("Test"))
                .build();
        assertEquals(
                Level.DEBUG,
                policy.rewrite(childLoggerEvent).getLevel(),
                "a rule for 'com.foo.' must still match its own child logger 'com.foo.Bar'");

        final LogEvent siblingPackageEvent = Log4jLogEvent.newBuilder()
                .setLoggerName("com.foobar.PaymentProcessor")
                .setLevel(Level.WARN)
                .setMessage(new SimpleMessage("Test"))
                .build();
        assertEquals(
                Level.WARN,
                policy.rewrite(siblingPackageEvent).getLevel(),
                "a rule for 'com.foo.' must not match the unrelated sibling package 'com.foobar'");
    }

    /**
     * An empty configured name is a catch-all: it must keep matching every logger name, as it did before
     * {@link #testPrefixMatchRespectsLoggerHierarchyBoundary}'s fix.
     */
    @Test
    void testEmptyPrefixStillMatchesEveryLogger() {
        final KeyValuePair[] rewrite = new KeyValuePair[] {new KeyValuePair("WARN", "DEBUG")};
        final LoggerNameLevelRewritePolicy policy = LoggerNameLevelRewritePolicy.createPolicy("", rewrite);

        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("com.foo.Bar")
                .setLevel(Level.WARN)
                .setMessage(new SimpleMessage("Test"))
                .build();
        assertEquals(Level.DEBUG, policy.rewrite(event).getLevel(), "an empty rule must match every logger name");
    }
}
