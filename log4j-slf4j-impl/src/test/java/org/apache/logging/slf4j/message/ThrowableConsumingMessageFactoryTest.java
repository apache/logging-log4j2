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
package org.apache.logging.slf4j.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory2;
import org.junit.jupiter.api.Test;

public class ThrowableConsumingMessageFactoryTest {

    private static final String MESSAGE = "MESSAGE";
    private static final Object P0 = new Object();
    private static final Object P1 = new Object();
    private static final Object P2 = new Object();
    private static final Object P3 = new Object();
    private static final Object P4 = new Object();
    private static final Object P5 = new Object();
    private static final Object P6 = new Object();
    private static final Object P7 = new Object();
    private static final Object P8 = new Object();
    private static final Object P9 = new Object();
    private static final Object P10 = new Object();
    private static final Object THROWABLE = new Throwable();

    @Test
    void should_not_consume_last_object_parameter() {
        final MessageFactory2 factory = new ThrowableConsumingMessageFactory();
        assertThat(factory.newMessage(MESSAGE, P0))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(1, null);
        assertThat(factory.newMessage(MESSAGE, P0, P1))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(2, null);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(3, null);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(4, null);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(5, null);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, P5))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(6, null);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, P5, P6))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(7, null);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, P5, P6, P7))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(8, null);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, P5, P6, P7, P8))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(9, null);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(10, null);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(11, null);
    }

    @Test
    void should_consume_last_throwable_parameter() {
        final MessageFactory2 factory = new ThrowableConsumingMessageFactory();
        assertThat(factory.newMessage(MESSAGE, THROWABLE))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(0, THROWABLE);
        assertThat(factory.newMessage(MESSAGE, P0, THROWABLE))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(1, THROWABLE);
        assertThat(factory.newMessage(MESSAGE, P0, P1, THROWABLE))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(2, THROWABLE);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, THROWABLE))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(3, THROWABLE);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, THROWABLE))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(4, THROWABLE);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, THROWABLE))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(5, THROWABLE);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, P5, THROWABLE))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(6, THROWABLE);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, P5, P6, THROWABLE))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(7, THROWABLE);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, P5, P6, P7, THROWABLE))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(8, THROWABLE);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, P5, P6, P7, P8, THROWABLE))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(9, THROWABLE);
        assertThat(factory.newMessage(MESSAGE, P0, P1, P2, P3, P4, P5, P6, P7, P8, P9, THROWABLE))
                .extracting(m -> m.getParameters().length, Message::getThrowable)
                .as("checking parameter count and throwable")
                .containsExactly(10, THROWABLE);
    }
}
