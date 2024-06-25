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
package org.apache.logging.slf4j;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.jspecify.annotations.NullMarked;

/**
 * Bind the Log4j API to SLF4J.
 */
@NullMarked
@ServiceProvider(value = Provider.class, resolution = Resolution.OPTIONAL)
public class SLF4JProvider extends Provider {

    private static final LoggerContextFactory CONTEXT_FACTORY = new SLF4JLoggerContextFactory();
    private static final ThreadContextMap THREAD_CONTEXT_MAP = new MDCContextMap();

    public SLF4JProvider() {
        super(15, CURRENT_VERSION, SLF4JLoggerContextFactory.class, MDCContextMap.class);
    }

    @Override
    public LoggerContextFactory getLoggerContextFactory() {
        return CONTEXT_FACTORY;
    }

    @Override
    public ThreadContextMap getThreadContextMapInstance() {
        return THREAD_CONTEXT_MAP;
    }
}
