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

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.impl.Log4jProvider;
import org.apache.logging.log4j.util.ProviderUtil;

/**
 * <p>
 * Utility class to access package protected methods in {@code ThreadContext}.
 * </p>
 *
 * @see ThreadContext
 * @since 2.7
 */
public final class ThreadContextTestAccess {
    private ThreadContextTestAccess() { // prevent instantiation
    }

    public static void init() {
        final Log4jProvider provider = (Log4jProvider) ProviderUtil.getProvider();
        try {
            final Method method = Log4jProvider.class.getDeclaredMethod("resetThreadContextMap");
            method.setAccessible(true);
            method.invoke(provider);
        } catch (Exception ex) {
            fail("Unable to reset ThreadContextMap");
        }
        ThreadContext.init();
    }
}
