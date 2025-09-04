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
package org.apache.logging.log4j.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.io.PrintStream;
import java.util.List;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j2-streams-calling-info.xml")
public class IoBuilderTest {

    private LoggerContext context = null;

    IoBuilderTest(LoggerContext context) {
        this.context = context;
    }

    @Test
    public void testNoArgBuilderCallerClassInfo() {
        try (final PrintStream ps = IoBuilder.forLogger().buildPrintStream()) {
            ps.println("discarded");
            final ListAppender app = context.getConfiguration().getAppender("IoBuilderTest");
            final List<String> messages = app.getMessages();
            assertThat(messages, not(empty()));
            assertThat(messages, hasSize(1));
            final String message = messages.get(0);
            assertThat(message, startsWith(getClass().getName() + ".testNoArgBuilderCallerClassInfo"));
            app.clear();
        }
    }
}
