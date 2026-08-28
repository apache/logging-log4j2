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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Constants;
import org.junit.jupiter.api.Test;

public class StatusDataTest {
    @Test
    void test_getFormattedData_does_not_throw() {
        // ensure that getFormattedData() does not throw an ArrayIndexOutOfBoundsException when given
        // a message with a 0-length (non-null) parameters array

        Message message = new Message() {
            @Override
            public String getFormattedMessage() {
                return "formatted";
            }

            @Override
            public Object[] getParameters() {
                return Constants.EMPTY_OBJECT_ARRAY;
            }

            @Override
            public Throwable getThrowable() {
                return null;
            }
        };

        StatusData statusData = new StatusData(null, Level.ERROR, message, null, null);
        assertThat(statusData.getFormattedStatus()).contains("formatted");
    }
}
