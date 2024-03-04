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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory;
import org.junit.jupiter.api.Test;

public class StatusConsoleListenerTest {

    public static final MessageFactory MESSAGE_FACTORY = ParameterizedNoReferenceMessageFactory.INSTANCE;

    @Test
    void StatusData_getFormattedStatus_should_be_used() {

        // Create the listener.
        final PrintStream stream = mock(PrintStream.class);
        final StatusConsoleListener listener = new StatusConsoleListener(Level.ALL, stream);

        // Log a message.
        final Message message = mock(Message.class);
        final StatusData statusData = spy(new StatusData(null, Level.TRACE, message, null, null));
        listener.log(statusData);

        // Verify the call.
        verify(statusData).getFormattedStatus();
    }

    @Test
    void stream_should_be_honored() throws Exception {

        // Create the listener.
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final String encoding = "UTF-8";
        final PrintStream printStream = new PrintStream(outputStream, false, encoding);
        final StatusConsoleListener listener = new StatusConsoleListener(Level.WARN, printStream);

        // log a message that is expected to be logged.
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

        // Collect the output.
        printStream.flush();
        final String output = outputStream.toString(encoding);

        // Verify the output.
        assertThat(output).contains(expectedThrowable.getMessage()).contains(expectedMessage.getFormattedMessage());
    }

    @Test
    void non_system_streams_should_be_closed() {
        final PrintStream stream = mock(PrintStream.class);
        final StatusConsoleListener listener = new StatusConsoleListener(Level.WARN, stream);
        listener.close();
        verify(stream).close();
    }

    @Test
    void close_should_reset_to_initials() {

        // Create the listener
        final PrintStream initialStream = mock(PrintStream.class);
        final Level initialLevel = Level.TRACE;
        final StatusConsoleListener listener = new StatusConsoleListener(initialLevel, initialStream);

        // Verify the initial state
        assertThat(listener.getStatusLevel()).isEqualTo(initialLevel);
        assertThat(listener).hasFieldOrPropertyWithValue("stream", initialStream);

        // Update the state
        final PrintStream newStream = mock(PrintStream.class);
        listener.setStream(newStream);
        final Level newLevel = Level.DEBUG;
        listener.setLevel(newLevel);

        // Verify the update
        verify(initialStream).close();
        assertThat(listener.getStatusLevel()).isEqualTo(newLevel);
        assertThat(listener).hasFieldOrPropertyWithValue("stream", newStream);

        // Close the listener
        listener.close();

        // Verify the reset
        verify(newStream).close();
        assertThat(listener.getStatusLevel()).isEqualTo(initialLevel);
        assertThat(listener).hasFieldOrPropertyWithValue("stream", initialStream);
    }
}
