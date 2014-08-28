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
package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Log4j Constants.
 */
public interface Constants {

    /**
     * Name of the system property to use to identify the LogEvent factory.
     */
    String LOG4J_LOG_EVENT_FACTORY = "Log4jLogEventFactory";

    /**
     * Name of the system property to use to identify the ContextSelector Class.
     */
    String LOG4J_CONTEXT_SELECTOR = "Log4jContextSelector";

    String LOG4J_DEFAULT_STATUS_LEVEL = "Log4jDefaultStatusLevel";

    /**
     * JNDI context name string literal.
     */
    String JNDI_CONTEXT_NAME = "java:comp/env/log4j/context-name";

    /**
     * Line separator.
     */
    String LINE_SEPARATOR = PropertiesUtil.getProperties().getStringProperty("line.separator", "\n");

    /**
     * Number of milliseconds in a second.
     */
    int MILLIS_IN_SECONDS = 1000;
}
