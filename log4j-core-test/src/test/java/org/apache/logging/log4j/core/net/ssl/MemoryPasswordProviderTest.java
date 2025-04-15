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
package org.apache.logging.log4j.core.net.ssl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MemoryPasswordProviderTest {
    @Test
    void testConstructorAllowsNull() {
        assertNull(new MemoryPasswordProvider(null).getPassword());
    }

    @Test
    void testConstructorDoesNotModifyOriginalParameterArray() {
        final char[] initial = "123".toCharArray();
        new MemoryPasswordProvider(initial);
        assertArrayEquals("123".toCharArray(), initial);
    }

    @Test
    void testGetPasswordReturnsCopyOfConstructorArray() {
        final char[] initial = "123".toCharArray();
        final MemoryPasswordProvider provider = new MemoryPasswordProvider(initial);
        final char[] actual = provider.getPassword();
        assertArrayEquals("123".toCharArray(), actual);
        assertNotSame(initial, actual);

        Arrays.fill(initial, 'a');
        assertArrayEquals("123".toCharArray(), provider.getPassword());
        assertNotSame(provider.getPassword(), provider.getPassword());
    }
}
