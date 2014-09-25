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
package org.apache.logging.log4j.junit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.junit.rules.ExternalResource;

/**
 * Sets the {@link LogManager}'s {@link LoggerContextFactory} to the given instance before the test and restores it to
 * the original value after the test.
 */
public class LogManagerLoggerContextFactoryRule extends ExternalResource {

    private final LoggerContextFactory loggerContextFactory;

    private LoggerContextFactory restoreLoggerContextFactory;

    public LogManagerLoggerContextFactoryRule(final LoggerContextFactory loggerContextFactory) {
        super();
        this.loggerContextFactory = loggerContextFactory;
    }

    @Override
    protected void after() {
        LogManager.setFactory(this.restoreLoggerContextFactory);
    }

    @Override
    protected void before() throws Throwable {
        this.restoreLoggerContextFactory = LogManager.getFactory();
        LogManager.setFactory(this.loggerContextFactory);
    }

}
