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
package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.spi.TraceContextProvider;

/**
 * A test-only TraceContextProvider designed to simulate thread-local trace contexts.
 * Mimics active span context lookups in systems like OpenTelemetry or Micrometer.
 */
public class TestTraceContextProvider implements TraceContextProvider {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SPAN_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TRACE_FLAGS = new ThreadLocal<>();

    public static void setContext(final String traceId, final String spanId, final String traceFlags) {
        TRACE_ID.set(traceId);
        SPAN_ID.set(spanId);
        TRACE_FLAGS.set(traceFlags);
    }

    public static void clearContext() {
        TRACE_ID.remove();
        SPAN_ID.remove();
        TRACE_FLAGS.remove();
    }

    @Override
    public String getTraceId() {
        return TRACE_ID.get();
    }

    @Override
    public String getSpanId() {
        return SPAN_ID.get();
    }

    @Override
    public String getTraceFlags() {
        return TRACE_FLAGS.get();
    }
}
