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
package org.apache.logging.log4j.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.apache.logging.log4j.ScopedContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j-list2.xml")
public class ScopedContextTest {

    private final ListAppender app;

    public ScopedContextTest(@Named("List") final ListAppender list) {
        app = list.clear();
    }

    @Test
    public void testScope(final LoggerContext context) throws Exception {
        final org.apache.logging.log4j.Logger logger = context.getLogger("org.apache.logging.log4j.scoped");
        ScopedContext.where("key1", "Log4j2").run(() -> logger.debug("Hello, {}", "World"));
        List<String> msgs = app.getMessages();
        assertThat(msgs, hasSize(1));
        String expected = "{key1=Log4j2}";
        assertThat(msgs.get(0), containsString(expected));
        app.clear();
        ScopedContext.runWhere("key1", "value1", () -> {
            logger.debug("Log message 1 will include key1");
            ScopedContext.runWhere("key2", "value2", () -> logger.debug("Log message 2 will include key1 and key2"));
            int count = 0;
            try {
                count = ScopedContext.callWhere("key2", "value2", () -> {
                    logger.debug("Log message 2 will include key2");
                    return 3;
                });
            } catch (Exception e) {
                fail("Caught Exception: " + e.getMessage());
            }
            assertThat(count, equalTo(3));
        });
        msgs = app.getMessages();
        assertThat(msgs, hasSize(3));
        expected = "{key1=value1}";
        assertThat(msgs.get(0), containsString(expected));
        expected = "{key1=value1, key2=value2}";
        assertThat(msgs.get(1), containsString(expected));
        assertThat(msgs.get(2), containsString(expected));
    }
}
