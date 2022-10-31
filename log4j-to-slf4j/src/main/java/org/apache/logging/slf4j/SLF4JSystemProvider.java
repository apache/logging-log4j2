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

package org.apache.logging.slf4j;

import org.apache.logging.log4j.spi.AbstractLoggingSystemProvider;
import org.apache.logging.log4j.spi.ThreadContextMap;

/**
 * Bind the Log4j API to SLF4J.
 */
public class SLF4JSystemProvider extends AbstractLoggingSystemProvider<SLF4JLoggerContextFactory> {
    @Override
    protected SLF4JLoggerContextFactory createLoggerContextFactory() {
        return new SLF4JLoggerContextFactory();
    }

    @Override
    protected ThreadContextMap.Factory createContextMapFactory() {
        return new MDCContextMap.Factory();
    }

    @Override
    public int getPriority() {
        return 15;
    }

    @Override
    public String getVersion() {
        return "2.6.0";
    }
}
