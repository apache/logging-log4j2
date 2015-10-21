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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class LookupTest {

    private static final String CONFIG = "log4j-lookup.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Test
    public void testHostname() {
        final ConsoleAppender app = context.getRequiredAppender("console", ConsoleAppender.class);
        final Layout<?> layout = app.getLayout();
        assertNotNull("No Layout", layout);
        assertTrue("Layout is not a PatternLayout", layout instanceof PatternLayout);
        final String pattern = ((PatternLayout) layout).getConversionPattern();
        assertNotNull("No conversion pattern", pattern);
        assertTrue("No filters", pattern.contains("org.junit,org.apache.maven,org.eclipse,sun.reflect,java.lang.reflect"));
    }
}
