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
package org.apache.logging.log4j.core.async.perftest;

/**
 * Idle strategy for use by threads when they do not have work to do.
 *
 * <h3>Note regarding potential for TTSP(Time To Safe Point) issues</h3>
 *
 * If the caller spins in a 'counted' loop, and the implementation does not include a a safepoint poll this may cause a TTSP
 * (Time To SafePoint) problem. If this is the case for your application you can solve it by preventing the idle method from
 * being inlined by using a Hotspot compiler command as a JVM argument e.g:
 * <code>-XX:CompileCommand=dontinline,org.apache.logging.log4j.core.async.perftest.NoOpIdleStrategy::idle</code>
 *
 * @see <a href="https://github.com/real-logic/Agrona/blob/master/src/main/java/org/agrona/concurrent/IdleStrategy.java">
 *     https://github.com/real-logic/Agrona/blob/master/src/main/java/org/agrona/concurrent/IdleStrategy.java</a>
 */
interface IdleStrategy {
    /**
     * Perform current idle action (e.g. nothing/yield/sleep).
     */
    void idle();
}
