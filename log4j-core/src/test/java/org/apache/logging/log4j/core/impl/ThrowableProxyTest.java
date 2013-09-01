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

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 *
 */
public class ThrowableProxyTest {

    @Test
    public void testStack() {
        final Map<String, ThrowableProxy.CacheEntry> map = new HashMap<String, ThrowableProxy.CacheEntry>();
        final Stack<Class<?>> stack = new Stack<Class<?>>();
        final Throwable throwable = new IllegalStateException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        final StackTracePackageElement[] callerPackageData = proxy.resolvePackageData(stack, map, null,
            throwable.getStackTrace());
        Assert.assertNotNull("No package data returned", callerPackageData);
    }
}
