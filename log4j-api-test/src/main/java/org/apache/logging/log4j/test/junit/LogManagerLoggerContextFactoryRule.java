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
package org.apache.logging.log4j.test.junit;

import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.util3.LoggingSystem;
import org.junit.rules.ExternalResource;

/**
 * Sets the {@link LoggingSystem}'s {@link LoggerContextFactory} to the given instance before the test and restores it to
 * the original value after the test.
 *
 * @deprecated Use {@link LoggerContextFactoryExtension} with JUnit 5
 */
@Deprecated
public class LogManagerLoggerContextFactoryRule extends ExternalResource {

    private final LoggerContextFactory loggerContextFactory;

    private LoggerContextFactory restoreLoggerContextFactory;

    public LogManagerLoggerContextFactoryRule(final LoggerContextFactory loggerContextFactory) {
        super();
        this.loggerContextFactory = loggerContextFactory;
    }

    @Override
    protected void after() {
        LoggingSystem.getInstance().setLoggerContextFactory(this.restoreLoggerContextFactory);
    }

    @Override
    protected void before() throws Throwable {
        final LoggingSystem system = LoggingSystem.getInstance();
        this.restoreLoggerContextFactory = system.getLoggerContextFactory();
        system.setLoggerContextFactory(this.loggerContextFactory);
    }

}
