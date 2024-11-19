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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import org.apache.logging.log4j.test.AbstractSerializationTest;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
public class MessageFormatMessageSerializationTest extends AbstractSerializationTest {

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {new MessageFormatMessage("Test")},
            {new MessageFormatMessage("Test {0} {1}", "message", "test")},
            {new MessageFormatMessage("{0}{1}{2}", 3, '.', 14L)}
        });
    }

    public MessageFormatMessageSerializationTest() {
        super();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testSerializationRoundtripEquals(Serializable serializable) {
        super.testSerializationRoundtripEquals(serializable);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testSerializationRoundtripNoException(Serializable serializable) {
        super.testSerializationRoundtripNoException(serializable);
    }
}
