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
package org.apache.logging.log4j.core.appender;

/**
 * This class captures possible performance hints that may be used by some
 * runtimes to improve code performance. It is intended to capture hinting
 * behaviors that are implemented in or anticipated to be spec'ed under the
 * java.lang.Thread class in some Java SE versions, but missing in prior
 * versions.
 *
 * Written by Gil Tene, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */
final class ThreadHints {

    private static final boolean okToInvokeThreadHintsMHCaller;

    static {
        boolean okToInvokeMHCaller;

        try {
            Class.forName("java.lang.invoke.MethodHandle");
            okToInvokeMHCaller = true;
        } catch (Exception e) {
            okToInvokeMHCaller = false;
        }
        // okToInvokeThreadHintsMHCaller will be false for e.g. Java SE 6 and earlier:
        okToInvokeThreadHintsMHCaller = okToInvokeMHCaller;
    }

    // prevent construction...
    private ThreadHints() {
    }

    /**
     * Indicates that the caller is momentarily unable to progress, until the
     * occurrence of one or more actions on the part of other activities.  By
     * invoking this method within each iteration of a spin-wait loop construct,
     * the calling thread indicates to the runtime that it is busy-waiting. The runtime
     * may take action to improve the performance of invoking spin-wait loop
     * constructions.
     */
    public static void onSpinWait() {
        if (okToInvokeThreadHintsMHCaller) {
            ThreadHintsMH.onSpinWait();
        }
    }
}