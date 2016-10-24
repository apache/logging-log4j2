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
package org.apache.logging.log4j.perf.nogc;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.Message;

/**
 * Logic common among ClassicLogger and NoGcLogger.
 */
public abstract class AbstractLogger {
    private final MutableLogEvent reusedLogEvent = new MutableLogEvent();
    protected DemoAppender appender = new DemoAppender(createLayout());

    protected abstract Layout<?> createLayout();

    protected void log(final Message message) {
        callAppenders(createLogEvent(message));
    }

    private void callAppenders(final LogEvent logEvent) {
        appender.append(logEvent);
    }

    private LogEvent createLogEvent(final Message message) {
        reusedLogEvent.setMessage(message);
        return reusedLogEvent;
    }

}
