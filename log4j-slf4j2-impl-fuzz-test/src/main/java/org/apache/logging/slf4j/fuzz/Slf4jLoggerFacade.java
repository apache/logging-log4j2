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
package org.apache.logging.slf4j.fuzz;

import static java.util.Objects.requireNonNull;

import org.apache.logging.log4j.fuzz.FuzzingUtil.LoggerFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.CallerBoundaryAware;
import org.slf4j.spi.LoggingEventBuilder;

public final class Slf4jLoggerFacade implements LoggerFacade {

    private static final String FQCN = Slf4jLoggerFacade.class.getName();

    private final Logger logger;

    private Slf4jLoggerFacade(final Logger logger) {
        this.logger = logger;
    }

    static Slf4jLoggerFacade ofClass(final Class<?> clazz) {
        requireNonNull(clazz, "clazz");
        final Logger logger = LoggerFactory.getLogger(clazz);
        return new Slf4jLoggerFacade(logger);
    }

    private LoggingEventBuilder atError() {
        final LoggingEventBuilder builder = logger.atError();
        if (builder instanceof CallerBoundaryAware) {
            ((CallerBoundaryAware) builder).setCallerBoundary(FQCN);
        }
        return builder;
    }

    @Override
    public void log(final String message) {
        atError().log(message);
    }

    @Override
    public void log(final String message, final Throwable throwable) {
        atError().setCause(throwable).log(message);
    }

    @Override
    public void log(final String message, final Object[] parameters) {
        atError().log(message, parameters);
    }
}
