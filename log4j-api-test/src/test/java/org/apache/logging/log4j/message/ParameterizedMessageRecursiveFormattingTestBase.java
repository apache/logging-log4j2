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

import org.apache.logging.log4j.util.Constants;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ParameterizedMessage#getFormattedMessage()} when formatted arguments causes another {@code ParameterizedMessage#getFormattedMessage()} (i.e., recursive) invocation.
 */
abstract class ParameterizedMessageRecursiveFormattingTestBase {

    private final boolean threadLocalsEnabled;

    ParameterizedMessageRecursiveFormattingTestBase(boolean threadLocalsEnabled) {
        this.threadLocalsEnabled = threadLocalsEnabled;
    }

    @Test
    void thread_locals_toggle_should_match() {
        assertThat(Constants.ENABLE_THREADLOCALS).isEqualTo(threadLocalsEnabled);
    }

    @Test
    void recursion_should_not_corrupt_formatting() {
        final Object argInvokingParameterizedMessageFormatting = new Object() {
            @Override
            public String toString() {
                return new ParameterizedMessage("bar {}", "baz").getFormattedMessage();
            }
        };
        final ParameterizedMessage message =
                new ParameterizedMessage("foo {}", argInvokingParameterizedMessageFormatting);
        final String actualText = message.getFormattedMessage();
        assertThat(actualText).isEqualTo("foo bar baz");
    }
}
