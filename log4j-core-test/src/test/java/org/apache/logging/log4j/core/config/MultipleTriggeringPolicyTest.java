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

import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests related to <a href="https://issues.apache.org/jira/browse/LOG4J2-1100">LOG4J2-1100</a>.
 */
class MultipleTriggeringPolicyTest {
    @Test
    @LoggerContextSource("LOG4J2-1100/log4j2.xml")
    void xml(final Configuration configuration) {
        assertBothTriggeringPoliciesConfigured(configuration);
    }

    @Test
    @Tag("json")
    @LoggerContextSource("LOG4J2-1100/log4j2.json")
    void json(final Configuration configuration) {
        assertBothTriggeringPoliciesConfigured(configuration);
    }

    @Test
    @Tag("yaml")
    @LoggerContextSource("LOG4J2-1100/log4j2-good.yaml")
    void yaml(final Configuration configuration) {
        assertBothTriggeringPoliciesConfigured(configuration);
    }

    @Test
    @Tag("yaml")
    @Disabled("LOG4J2-1100 demonstration")
    @LoggerContextSource("LOG4J2-1100/log4j2-good.yaml")
    void unsupportedYamlSyntax(final Configuration configuration) {
        assertBothTriggeringPoliciesConfigured(configuration);
    }

    void assertBothTriggeringPoliciesConfigured(final Configuration configuration) {
        final RollingFileAppender appender = configuration.getAppender("File");
        assertNotNull(appender);
        final CompositeTriggeringPolicy compositeTriggeringPolicy = appender.getTriggeringPolicy();
        assertNotNull(compositeTriggeringPolicy);
        final TriggeringPolicy[] triggeringPolicies = compositeTriggeringPolicy.getTriggeringPolicies();
        assertEquals(2, triggeringPolicies.length);
        final SizeBasedTriggeringPolicy sizeBasedTriggeringPolicy;
        final TimeBasedTriggeringPolicy timeBasedTriggeringPolicy;
        if (triggeringPolicies[0] instanceof SizeBasedTriggeringPolicy) {
            sizeBasedTriggeringPolicy = (SizeBasedTriggeringPolicy) triggeringPolicies[0];
            timeBasedTriggeringPolicy = (TimeBasedTriggeringPolicy) triggeringPolicies[1];
        } else {
            sizeBasedTriggeringPolicy = (SizeBasedTriggeringPolicy) triggeringPolicies[1];
            timeBasedTriggeringPolicy = (TimeBasedTriggeringPolicy) triggeringPolicies[0];
        }
        assertEquals(7, timeBasedTriggeringPolicy.getInterval());
        assertEquals(100 * 1024 * 1024, sizeBasedTriggeringPolicy.getMaxFileSize());
    }
}
