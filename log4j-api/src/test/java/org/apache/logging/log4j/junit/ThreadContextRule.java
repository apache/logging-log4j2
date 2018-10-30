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
package org.apache.logging.log4j.junit;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContextHolder;
import org.junit.rules.ExternalResource;

/**
 * Restores the ThreadContext to it's initial map and stack values after a JUnit test.
 * 
 * Usage:
 * 
 * <pre>
 * &#64;Rule
 * public final ThreadContextRule threadContextRule = new ThreadContextRule();
 * </pre>
 */
public class ThreadContextRule extends ExternalResource {

    private final boolean restoreMap;
    private final boolean restoreStack;
    private ThreadContextHolder threadContextHolder;

    /**
     * Constructs an instance initialized to restore the stack and map.
     */
    public ThreadContextRule() {
        this(true, true);
    }

    /**
     * Constructs an instance initialized to restore the given structures.
     * 
     * @param restoreMap
     *            Whether to restore the thread context map.
     * @param restoreStack
     *            Whether to restore the thread context stack.
     */
    public ThreadContextRule(final boolean restoreMap, final boolean restoreStack) {
        super();
        this.restoreMap = restoreMap;
        this.restoreStack = restoreStack;
    }

    @Override
    protected void after() {
        if (threadContextHolder != null) {
            threadContextHolder.restore();
        }
    }

    @Override
    protected void before() throws Throwable {
        threadContextHolder = new ThreadContextHolder(restoreMap, restoreStack);
        if (restoreMap) {
            ThreadContext.clearMap();
        }
        if (restoreStack) {
            ThreadContext.clearStack();
        }
    }

}
