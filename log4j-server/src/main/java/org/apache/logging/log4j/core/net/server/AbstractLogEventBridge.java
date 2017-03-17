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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Abstract class for implementations of {@link LogEventBridge}.
 * 
 * @param <T>
 *            The kind of input stream read
 */
public abstract class AbstractLogEventBridge<T extends InputStream> implements LogEventBridge<T> {

    protected static final int END = -1;

    protected static final Logger logger = StatusLogger.getLogger();

    // The default is to return the same object as given.
    @SuppressWarnings("unchecked")
    @Override
    public T wrapStream(final InputStream inputStream) throws IOException {
        return (T) inputStream;
    }

}
