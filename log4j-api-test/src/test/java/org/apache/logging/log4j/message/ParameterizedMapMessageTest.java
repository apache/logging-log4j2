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
package org.apache.logging.log4j.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

@UsingStatusListener
class ParameterizedMapMessageTest {

    final ListStatusListener statusListener;

    ParameterizedMapMessageTest(ListStatusListener statusListener) {
        this.statusListener = statusListener;
    }

    @Test
    void testNoArgs() {
        final String testMsg = "Test message {}";
        ParameterizedMessage msg = new ParameterizedMessage(testMsg, (Object[]) null);
        String result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
        final Object[] array = null;
        msg = new ParameterizedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
    }

    @Test
    void testZeroLength() {
        final String testMsg = "";
        ParameterizedMessage msg = new ParameterizedMessage(testMsg, new Object[] {"arg"});
        String result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
        final Object[] array = null;
        msg = new ParameterizedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
    }

    @Test
    void testOneCharLength() {
        final String testMsg = "d";
        ParameterizedMessage msg = new ParameterizedMessage(testMsg, new Object[] {"arg"});
        String result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
        final Object[] array = null;
        msg = new ParameterizedMessage(testMsg, array, null);
        result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
    }

    @Test
    void testFormat3StringArgs() {
        final String testMsg = "Test message {}{} {}";
        final String[] args = {"a", "b", "c"};
        final String result = ParameterizedMessage.format(testMsg, args);
        assertThat(result).isEqualTo("Test message ab c");
    }
}
