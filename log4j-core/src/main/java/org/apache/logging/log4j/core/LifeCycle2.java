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

import java.util.concurrent.TimeUnit;

/**
 * Extends the LifeCycle interface.
 * <p>
 *  This interface should be merged with the super-interface in 3.0.
 * </p>
 * @since 2.7
 */
public interface LifeCycle2 extends LifeCycle {

    /**
     * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current
     * thread is interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param timeUnit the time unit of the timeout argument
     * @return true if the receiver was stopped cleanly and normally, false otherwise.
     * @since 2.7
     */
    boolean stop(long timeout, TimeUnit timeUnit);
}
