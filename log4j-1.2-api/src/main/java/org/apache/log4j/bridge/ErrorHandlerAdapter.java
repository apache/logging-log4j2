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
package org.apache.log4j.bridge;

import org.apache.log4j.spi.ErrorHandler;
import org.apache.logging.log4j.core.LogEvent;

/**
 * Makes a Log4j 1 ErrorHandler usable by a Log4j 2 Appender.
 */
public class ErrorHandlerAdapter implements org.apache.logging.log4j.core.ErrorHandler {

    private final ErrorHandler errorHandler;

    public ErrorHandlerAdapter(final ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public ErrorHandler getHandler() {
        return errorHandler;
    }

    @Override
    public void error(final String msg) {
        errorHandler.error(msg);
    }

    @Override
    public void error(final String msg, final Throwable t) {
        if (t instanceof Exception) {
            errorHandler.error(msg, (Exception) t, 0);
        } else {
            errorHandler.error(msg);
        }
    }

    @Override
    public void error(final String msg, final LogEvent event, final Throwable t) {
        if (t == null || t instanceof Exception) {
            errorHandler.error(msg, (Exception) t, 0, new LogEventAdapter(event));
        } else {
            errorHandler.error(msg);
        }
    }
}
