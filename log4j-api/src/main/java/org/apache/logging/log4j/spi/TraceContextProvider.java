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
package org.apache.logging.log4j.spi;

/**
 * Service Provider Interface (SPI) for retrieving distributed tracing metadata (such as W3C Trace Context)
 * from the active execution context.
 * <p>
 * Implementing this SPI allows tracing frameworks (e.g., OpenTelemetry, Micrometer, Zipkin) to pass native
 * trace identifiers directly to Log4j events. This completely bypasses the {@link org.apache.logging.log4j.ThreadContext}
 * map, eliminating map-cloning and garbage collection overhead during asynchronous logging.
 * </p>
 * <p>
 * Log4j locates implementations of this interface using the standard Java {@link java.util.ServiceLoader} mechanism.
 * To register a custom provider, create a plain-text file named
 * {@code org.apache.logging.log4j.spi.TraceContextProvider} in the {@code META-INF/services/} directory
 * containing the fully qualified class name of the implementation.
 * </p>
 *
 * @since 2.27.0
 */
public interface TraceContextProvider {

    /**
     * Returns the standard trace ID from the active context, or {@code null}.
     */
    String getTraceId();

    /**
     * Returns the standard span ID from the active context, or {@code null}.
     */
    String getSpanId();

    /**
     * Returns the standard trace flags from the active context, or {@code null}.
     */
    String getTraceFlags();
}
