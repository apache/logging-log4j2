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
package org.apache.logging.log4j.core.lookup;

import java.util.Map;

/**
 * Work in progress, saved for future experimentation.
 * 
 * TODO The goal is to use the Sun debugger API to find the main arg values on the stack.
 */
public class MainInputArgumentsMapLookup extends MapLookup {

    public static final MainInputArgumentsMapLookup SINGLETON_STACK;

    static {
        final Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        final String[] args = null;
        for (final Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            final StackTraceElement[] stackTraceElements = entry.getValue();
            entry.getKey();
            // Can't use the thread name to look for "main" since anyone can set it.
            // Can't use thread ID since it can be any positive value, and is likely vender dependent. Oracle seems to
            // use 1.
            // We are left to look for "main" at the top of the stack
            if (stackTraceElements != null) {
                final int frame0 = stackTraceElements.length - 1;
                if ("main".equals(stackTraceElements[frame0].getMethodName())) {
                    // We could further validate the main is a public static void method that takes a String[], if not,
                    // look at the other threads.
                    //
                    // How do we get the main args from the stack with the debug API?
                    // Must we be started in debug mode? Seems like it.
                }
            }
        }
        SINGLETON_STACK = new MainInputArgumentsMapLookup(MapLookup.toMap(args));
    }

    public MainInputArgumentsMapLookup(final Map<String, String> map) {
        super(map);
    }
}
