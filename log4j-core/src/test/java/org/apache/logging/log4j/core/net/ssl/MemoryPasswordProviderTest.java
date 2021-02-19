package org.apache.logging.log4j.core.net.ssl;/*
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class MemoryPasswordProviderTest {
    @Test
    public void testConstructorAllowsNull() {
        assertThat(new MemoryPasswordProvider(null).getPassword()).isNull();
    }

    @Test
    public void testConstructorDoesNotModifyOriginalParameterArray() {
        char[] initial = "123".toCharArray();
        new MemoryPasswordProvider(initial);
        assertThat(initial).isEqualTo("123".toCharArray());
    }

    @Test
    public void testGetPasswordReturnsCopyOfConstructorArray() {
        char[] initial = "123".toCharArray();
        MemoryPasswordProvider provider = new MemoryPasswordProvider(initial);
        char[] actual = provider.getPassword();
        assertThat(actual).isEqualTo("123".toCharArray());
        assertThat(actual).isNotSameAs(initial);

        Arrays.fill(initial, 'a');
        assertThat(provider.getPassword()).isEqualTo("123".toCharArray());
        assertThat(provider.getPassword()).isNotSameAs(provider.getPassword());
    }
}
