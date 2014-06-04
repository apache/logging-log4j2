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
package org.apache.logging.log4j.taglib;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

/**
 * The bridge between the tag library and the Log4j API ensures that the source information for log events is
 * the JSP Servlet and not one of the taglib classes.
 *
 * @since 2.0
 */
class Log4jTaglibLogger extends ExtendedLoggerWrapper {
    private static final long serialVersionUID = 1L;

    public Log4jTaglibLogger(final ExtendedLogger logger, final String name, final MessageFactory messageFactory) {
        super(logger, name, messageFactory);
    }

    @Override
    protected void entry(final String fqcn, final Object... params) {
        super.entry(fqcn, params);
    }

    @Override
    protected <R> R exit(final String fqcn, final R result) {
        return super.exit(fqcn, result);
    }

    @Override
    protected void catching(final String fqcn, final Level level, final Throwable t) {
        super.catching(fqcn, level, t);
    }

    @Override
    protected <T extends Throwable> T throwing(final String fqcn, final Level level, final T t) {
        return super.throwing(fqcn, level, t);
    }
}
