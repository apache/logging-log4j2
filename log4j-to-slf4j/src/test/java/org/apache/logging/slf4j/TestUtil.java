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
package org.apache.logging.slf4j;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.testUtil.StringListAppender;
import org.slf4j.Logger;

/**
 * Utility methods for unit tests integrating with Logback.
 *
 * @since 2.1
 */
public final class TestUtil {

    public static StringListAppender<ILoggingEvent> getListAppender(final SLF4JLogger slf4jLogger, final String name) {
        final Logger logger = slf4jLogger.getLogger();
        if (!(logger instanceof AppenderAttachable)) {
            throw new AssertionError("SLF4JLogger.getLogger() did not return an instance of AppenderAttachable");
        }
        @SuppressWarnings("unchecked")
        final AppenderAttachable<ILoggingEvent> attachable = (AppenderAttachable<ILoggingEvent>) logger;
        return getListAppender(attachable, name);
    }

    public static StringListAppender<ILoggingEvent> getListAppender(final AppenderAttachable<ILoggingEvent> logger, final String name) {
        return (StringListAppender<ILoggingEvent>) logger.getAppender(name);
    }

    private TestUtil() {
    }
}
