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
package org.apache.logging.log4j.async.logger;

import com.lmax.disruptor.ExceptionHandler;
import org.apache.logging.log4j.kit.env.Log4jProperty;
import org.jspecify.annotations.Nullable;

public final class AsyncLoggerKeys {

    private AsyncLoggerKeys() {}

    public interface DisruptorProperties {
        @Nullable
        Class<? extends ExceptionHandler<?>> exceptionHandler();

        int retries();

        int ringBufferSize();

        long sleepTimeNS();

        boolean synchronizeEnqueueWhenQueueFull();

        long timeout();

        @Nullable
        String waitStrategy();
    }

    /**
     * Properties related to async loggers.
     */
    @Log4jProperty
    public record AsyncLogger(
            @Nullable Class<? extends ExceptionHandler<RingBufferLogEvent>> exceptionHandler,
            @Log4jProperty(defaultValue = "4096") int ringBufferSize,
            @Log4jProperty(defaultValue = "200") int retries,
            @Log4jProperty(defaultValue = "100") long sleepTimeNS,
            @Log4jProperty(defaultValue = "true") boolean synchronizeEnqueueWhenQueueFull,
            @Log4jProperty(defaultValue = "10") long timeout,
            @Nullable String waitStrategy)
            implements DisruptorProperties {}

    /**
     * Properties related to async logger configurations.
     */
    @Log4jProperty
    public record AsyncLoggerConfig(
            Class<? extends ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper>> exceptionHandler,
            @Log4jProperty(defaultValue = "4096") int ringBufferSize,
            @Log4jProperty(defaultValue = "200") int retries,
            @Log4jProperty(defaultValue = "100") long sleepTimeNS,
            @Log4jProperty(defaultValue = "true") boolean synchronizeEnqueueWhenQueueFull,
            @Log4jProperty(defaultValue = "10") long timeout,
            @Nullable String waitStrategy)
            implements DisruptorProperties {}

    public record WaitStrategy(
            @Nullable String name, @Nullable Long sleepTimeNs, @Nullable Integer retries, @Nullable Integer timeout) {}
}
