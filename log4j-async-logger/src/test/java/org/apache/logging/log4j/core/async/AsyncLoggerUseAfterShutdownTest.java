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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Test for <a href="https://issues.apache.org/jira/browse/LOG4J2-639">LOG4J2-639</a>
 */
@Tag("async")
@Tag("functional")
@ContextSelectorType(AsyncLoggerContextSelector.class)
public class AsyncLoggerUseAfterShutdownTest {

    @Test
    @LoggerContextSource
    public void testNoErrorIfLogAfterShutdown(final LoggerContext ctx) throws Exception {
        final Logger log = ctx.getLogger("com.foo.Bar");
        final String msg = "Async logger msg";
        log.info(msg, new InternalError("this is not a real error"));
        ctx.stop(); // stop async thread

        // call the #logMessage() method to bypass the isEnabled check:
        // before the LOG4J2-639 fix this would throw a NPE
        log.logMessage(Level.INFO, null, "com.foo.Bar", null, new SimpleMessage("msg"), null);
    }
}
