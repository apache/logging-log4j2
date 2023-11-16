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
package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Formats the event thread priority.
 *
 * @since 2.6
 */
@Plugin(name = "ThreadPriorityPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"tp", "threadPriority"})
@PerformanceSensitive("allocation")
public final class ThreadPriorityPatternConverter extends LogEventPatternConverter {
    /**
     * Singleton.
     */
    private static final ThreadPriorityPatternConverter INSTANCE = new ThreadPriorityPatternConverter();

    /**
     * Private constructor.
     */
    private ThreadPriorityPatternConverter() {
        super("ThreadPriority", "threadPriority");
    }

    /**
     * Obtains an instance of ThreadPatternConverter.
     *
     * @param options options, currently ignored, may be null.
     * @return instance of ThreadPatternConverter.
     */
    public static ThreadPriorityPatternConverter newInstance(final String[] options) {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        toAppendTo.append(event.getThreadPriority());
    }
}
