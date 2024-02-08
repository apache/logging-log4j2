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
package org.apache.logging.log4j.status;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class StatusConsoleListenerTest {

    public static final MessageFactory MESSAGE_FACTORY = ParameterizedNoReferenceMessageFactory.INSTANCE;

    @Test
    void StatusData_getFormattedStatus_should_be_used() {

        // Create the listener.
        final PrintStream stream = Mockito.mock(PrintStream.class);
        final StatusConsoleListener listener = new StatusConsoleListener(Level.ALL, stream);

        // Log a message.
        final Message message = Mockito.mock(Message.class);
        final StatusData statusData = Mockito.spy(new StatusData(null, Level.TRACE, message, null, null));
        listener.log(statusData);

        // Verify the call.
        Mockito.verify(statusData).getFormattedStatus();
    }

    @Test
    void level_and_stream_should_be_honored() throws Exception {

        // Create the listener.
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final String encoding = "UTF-8";
        final PrintStream printStream = new PrintStream(outputStream, false, encoding);
        final StatusConsoleListener listener = new StatusConsoleListener(Level.WARN, printStream);

        // First, log a message that is expected to be logged.
        final RuntimeException expectedThrowable = new RuntimeException("expectedThrowable");
        expectedThrowable.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("expectedThrowableClass", "expectedThrowableMethod", "expectedThrowableFile", 1)
        });
        final Message expectedMessage = MESSAGE_FACTORY.newMessage("expectedMessage");
        listener.log(new StatusData(
                null, // since ignored by `SimpleLogger`
                Level.WARN,
                expectedMessage,
                expectedThrowable,
                null)); // as set by `StatusLogger` itself

        // Second, log a message that is expected to be discarded due to its insufficient level.
        final RuntimeException discardedThrowable = new RuntimeException("discardedThrowable");
        discardedThrowable.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("discardedThrowableClass", "discardedThrowableMethod", "discardedThrowableFile", 2)
        });
        final Message discardedMessage = MESSAGE_FACTORY.newMessage("discardedMessage");
        listener.log(new StatusData(
                null, // since ignored by `SimpleLogger`
                Level.INFO,
                discardedMessage,
                discardedThrowable,
                null)); // as set by `StatusLogger` itself

        // Collect the output.
        printStream.flush();
        final String output = outputStream.toString(encoding);

        // Verify the output.
        Assertions.assertThat(output)
                .contains(expectedThrowable.getMessage())
                .contains(expectedMessage.getFormattedMessage())
                .doesNotContain(discardedThrowable.getMessage())
                .doesNotContain(discardedMessage.getFormattedMessage());
    }

    @Test
    void non_system_streams_should_be_closed() throws Exception {
        final PrintStream stream = Mockito.mock(PrintStream.class);
        final StatusConsoleListener listener = new StatusConsoleListener(Level.WARN, stream);
        listener.close();
        Mockito.verify(stream).close();
    }
}
