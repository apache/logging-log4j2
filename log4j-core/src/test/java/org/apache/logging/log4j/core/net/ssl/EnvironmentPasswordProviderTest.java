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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EnvironmentPasswordProviderTest {

    @Test
    public void testConstructorDisallowsNull() {
        assertThrows(NullPointerException.class, () -> new EnvironmentPasswordProvider(null));
    }

    @Test
    public void testGetPasswordReturnsEnvironmentVariableValue() {
        final String value = System.getenv("PATH");
        if (value == null) {
            return; // we cannot test in this environment
        }
        final char[] actual = new EnvironmentPasswordProvider("PATH").getPassword();
        assertArrayEquals(value.toCharArray(), actual);
    }
}