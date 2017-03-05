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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.rules.ExternalResource;

/**
 * JUnit rule to set a particular StatusLogger level. This rule is useful for debugging unit tests that do not use a
 * Log4j configuration file.
 *
 * @since 2.8
 */
public class StatusLoggerRule extends ExternalResource {

    private final StatusLogger statusLogger = StatusLogger.getLogger();
    private final Level level;
    private Level oldLevel;

    public StatusLoggerRule(final Level level) {
        this.level = level;
    }

    @Override
    protected void before() throws Throwable {
        oldLevel = statusLogger.getLevel();
        statusLogger.setLevel(level);
    }

    @Override
    protected void after() {
        statusLogger.setLevel(oldLevel);
    }
}
