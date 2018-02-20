/*
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
package org.apache.logging.log4j.jackson;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.logging.log4j.core.impl.ThrowableProxy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 */
public class ThrowableProxyJacksonTest {

    static class Fixture {
        @JsonProperty
        ThrowableProxy proxy = new ThrowableProxy(new IOException("test"));
    }

    protected void testIoContainer(final ObjectMapper objectMapper) throws IOException {
        final Fixture expected = new Fixture();
        final String s = objectMapper.writeValueAsString(expected);
        final Fixture actual = objectMapper.readValue(s, Fixture.class);
        assertEquals(expected.proxy.getName(), actual.proxy.getName());
        assertEquals(expected.proxy.getMessage(), actual.proxy.getMessage());
        assertEquals(expected.proxy.getLocalizedMessage(), actual.proxy.getLocalizedMessage());
        assertEquals(expected.proxy.getCommonElementCount(), actual.proxy.getCommonElementCount());
        assertArrayEquals(expected.proxy.getExtendedStackTrace(), actual.proxy.getExtendedStackTrace());
        assertEquals(expected.proxy, actual.proxy);
    }

}
