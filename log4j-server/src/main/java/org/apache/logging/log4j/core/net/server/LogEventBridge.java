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
package org.apache.logging.log4j.core.net.server;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LogEventListener;

/**
 * Reads {@link LogEvent}s from the given input stream and logs them as they are discovered on the given logger.
 * 
 * <p>
 * Should be stateless.
 * </p>
 * 
 * @param <T>
 *            The kind of {@link InputStream} to wrap and read.
 */
public interface LogEventBridge<T extends InputStream> {

    /**
     * Reads {@link LogEvent}s from the given input stream and logs them as they are discovered on the given logger.
     * 
     * @param inputStream
     *            the input stream to read
     * @param logEventListener
     *            TODO
     * @throws IOException
     */
    void logEvents(T inputStream, LogEventListener logEventListener) throws IOException;

    /**
     * Wraps the given stream if needed.
     * 
     * @param inputStream
     *            the stream to wrap
     * @return the wrapped stream or the given stream.
     * @throws IOException
     */
    T wrapStream(InputStream inputStream) throws IOException;
}
