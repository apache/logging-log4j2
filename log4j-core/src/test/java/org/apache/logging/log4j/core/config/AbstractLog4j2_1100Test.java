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

package org.apache.logging.log4j.core.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractLog4j2_1100Test {

    @Rule
    public LoggerContextRule context = new LoggerContextRule(getConfigurationResource());

    protected abstract String getConfigurationResource();

    @Test
    public void test() {
        final Configuration configuration = context.getConfiguration();
        assertNotNull(configuration);
        final RollingFileAppender appender = configuration.getAppender("File");
        assertNotNull(appender);
        final CompositeTriggeringPolicy compositeTriggeringPolicy = appender.getTriggeringPolicy();
        assertNotNull(compositeTriggeringPolicy);
        final TriggeringPolicy[] triggeringPolicies = compositeTriggeringPolicy.getTriggeringPolicies();
        SizeBasedTriggeringPolicy sizeBasedTriggeringPolicy = null;
        TimeBasedTriggeringPolicy timeBasedTriggeringPolicy = null;
        for (final TriggeringPolicy triggeringPolicy : triggeringPolicies) {
            if (triggeringPolicy instanceof TimeBasedTriggeringPolicy) {
                timeBasedTriggeringPolicy = (TimeBasedTriggeringPolicy) triggeringPolicy;
                assertEquals(7, timeBasedTriggeringPolicy.getInterval());
            }
            if (triggeringPolicy instanceof SizeBasedTriggeringPolicy) {
                sizeBasedTriggeringPolicy = (SizeBasedTriggeringPolicy) triggeringPolicy;
                assertEquals(100 * 1024 * 1024, sizeBasedTriggeringPolicy.getMaxFileSize());
            }
        }
        if (timeBasedTriggeringPolicy == null) {
            fail("Missing TimeBasedTriggeringPolicy");
        }
        if (sizeBasedTriggeringPolicy == null) {
            fail("Missing SizeBasedTriggeringPolicy");
        }
    }
}
