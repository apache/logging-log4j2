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
package org.apache.logging.log4j.core.appender.rolling;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.plugins.Factory;

/**
 * Provides a controllable clock for rolling appender tests.
 */
public abstract class AbstractRollingListenerTest implements RolloverListener {
    protected final AtomicLong currentTimeMillis = new AtomicLong(System.currentTimeMillis());

    @Factory
    Clock clock() {
        return currentTimeMillis::get;
    }

    @Override
    public void rolloverTriggered(final String fileName) {
    }
}
