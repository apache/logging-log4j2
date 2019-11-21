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
package org.apache.logging.log4j.io;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import java.io.PrintStream;
import java.util.List;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;

public class IoBuilderTest {

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule("log4j2-streams-calling-info.xml");

    @Test
    public void testNoArgBuilderCallerClassInfo() throws Exception {
        try (final PrintStream ps = IoBuilder.forLogger().buildPrintStream()) {
            ps.println("discarded");
            final ListAppender app = context.getListAppender("IoBuilderTest");
            final List<String> messages = app.getMessages();
            assertThat(messages, not(empty()));
            assertThat(messages, hasSize(1));
            final String message = messages.get(0);
            assertThat(message, startsWith(getClass().getName() + ".testNoArgBuilderCallerClassInfo"));
            app.clear();
        }
    }
}
