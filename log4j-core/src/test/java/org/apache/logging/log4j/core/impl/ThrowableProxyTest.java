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
package org.apache.logging.log4j.core.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 */
public class ThrowableProxyTest {

    static class Fixture {
        @JsonProperty
        ThrowableProxy proxy = new ThrowableProxy(new IOException("test"));
    }

    @Test
    public void testJsonIoContainer() throws IOException {
        ObjectMapper objectMapper = new Log4jJsonObjectMapper();
        Fixture expected = new Fixture();
        final String s = objectMapper.writeValueAsString(expected);
        Fixture actual = objectMapper.readValue(s, Fixture.class);
        Assert.assertEquals(expected.proxy.getName(), actual.proxy.getName());
        Assert.assertEquals(expected.proxy.getMessage(), actual.proxy.getMessage());
        Assert.assertEquals(expected.proxy.getLocalizedMessage(), actual.proxy.getLocalizedMessage());
        Assert.assertEquals(expected.proxy.getCommonElementCount(), actual.proxy.getCommonElementCount());
        Assert.assertArrayEquals(expected.proxy.getExtendedStackTrace(), actual.proxy.getExtendedStackTrace());        
        Assert.assertEquals(expected.proxy, actual.proxy);
    }

    @Test
    public void testStack() {
        final Map<String, ThrowableProxy.CacheEntry> map = new HashMap<String, ThrowableProxy.CacheEntry>();
        final Stack<Class<?>> stack = new Stack<Class<?>>();
        final Throwable throwable = new IllegalStateException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        final ExtendedStackTraceElement[] callerPackageData = proxy.toExtendedStackTrace(stack, map, null,
                throwable.getStackTrace());
        Assert.assertNotNull("No package data returned", callerPackageData);
    }
}
