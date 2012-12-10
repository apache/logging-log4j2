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
package org.apache.logging.log4j;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

/**
 *
 */
public class LogbackSubstitution {

    private static final String LOGBACK_CONF = "logback.configurationFile";
    private static final String LOGBACK_CONFIG = "logback-subst.xml";
    private final XLogger xLogger = XLoggerFactory.getXLogger(LogbackSubstitution.class);



    @BeforeClass
    public static void setupClass() {
        System.setProperty(LOGBACK_CONF, LOGBACK_CONFIG);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(LOGBACK_CONF);
    }

    @Test
    public void testSubst() {
        xLogger.debug("Hello, {}", "Log4j {}");
        xLogger.debug("Hello, {}");
    }
}
