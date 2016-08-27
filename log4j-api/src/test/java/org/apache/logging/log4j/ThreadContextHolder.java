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

package org.apache.logging.log4j;

import java.util.Map;

import org.apache.logging.log4j.ThreadContext.ContextStack;

/**
 * Holds an immutable copy of the ThreadContext stack and map.
 * 
 * TODO Use LOG4J2-1517 Add ThreadContext.setContext(Map<String, String>)
 * 
 * or
 * 
 * TODO Might be replaced by something from LOG4J2-1447.
 * 
 * or do nothing.
 * 
 * @since 2.7
 */
public class ThreadContextHolder {

    private final Map<String, String> immutableContext;
    private final ContextStack immutableStack;
    private final boolean restoreContext;
    private final boolean restoreStack;

    /**
     * Constructs a new holder initialized with an immutable copy of the ThreadContext stack and map.
     * 
     * @param restoreContext
     * @param restoreStack
     */
    public ThreadContextHolder(final boolean restoreContext, final boolean restoreStack) {
        this.restoreContext = restoreContext;
        this.restoreStack = restoreStack;
        this.immutableContext = restoreContext ? ThreadContext.getImmutableContext() : null;
        this.immutableStack = restoreStack ? ThreadContext.getImmutableStack() : null;
    }

    /**
     * Restores the ThreadContext stack and map based on the values saved in the constructor.
     */
    public void restore() {
        if (restoreStack) {
            ThreadContext.setStack(immutableStack);
        }
        if (restoreContext) {
            // TODO LOG4J2-1517 Add ThreadContext.setContext(Map<String, String>)
            // Use:
            // ThreadContext.setContext(immutableContext);
            // Instead of:
            ThreadContext.clearMap();
            ThreadContext.putAll(immutableContext);
            //
            // or:
            // ThreadContext.clearMap();
            // for (Map.Entry<String, String> entry : immutableContext.entrySet()) {
            // ThreadContext.put(entry.getKey(), entry.getValue());
            // }
        }
    }
}
