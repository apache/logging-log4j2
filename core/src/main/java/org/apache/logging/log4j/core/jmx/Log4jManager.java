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
package org.apache.logging.log4j.core.jmx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.List;

/**
 * Preliminary implementation for testing with JBoss.
 */
public class Log4jManager {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    public List<LoggerContext> getLoggerContexts() {
        final Log4jContextFactory factory = (Log4jContextFactory) LogManager.getFactory();
        final ContextSelector selector = factory.getSelector();
        return selector.getLoggerContexts();
    }

    public List<StatusData> getStatusData() {
        return LOGGER.getStatusData();
    }
}
