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

import static org.apache.logging.log4j.core.GcHelper.awaitGarbageCollection;

import org.apache.logging.log4j.message.ReusableMessage;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.junit.jupiter.api.Test;

class ReusableParameterizedMessageMemoryLeakTest {

    @Test
    void parameters_should_be_garbage_collected() throws Exception {
        awaitGarbageCollection(() -> {
            final ParameterObject parameter = new ParameterObject("paramValue");
            final ReusableMessage message =
                    (ReusableMessage) ReusableMessageFactory.INSTANCE.newMessage("foo {}", parameter);
            // Large enough for the parameters, but smaller than the default reusable array size
            message.swapParameters(new Object[5]);
            return parameter;
        });
    }

    private static final class ParameterObject {

        private final String value;

        private ParameterObject(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
