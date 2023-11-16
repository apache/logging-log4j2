/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Test SpringLookup.
 */
public class SpringLookupTest {

    @Test
    public void testLookup() {
        final MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setDefaultProfiles("one", "two");
        env.setProperty("app.property", "test");
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.putObject(Log4j2SpringBootLoggingSystem.ENVIRONMENT_KEY, env);
        final SpringLookup lookup = new SpringLookup();
        lookup.setLoggerContext(context);
        String result = lookup.lookup("profiles.active");
        assertThat(result).as("Incorrect active profile").isEqualTo("test");
        result = lookup.lookup("profiles.active[0]");
        assertThat(result).as("Incorrect active profile").isEqualTo("test");
        result = lookup.lookup("profiles.default");
        assertThat(result).as("Incorrect default profiles").isEqualTo("one,two");
        result = lookup.lookup("profiles.default[0]");
        assertThat(result).as("Incorrect default profiles").isEqualTo("one");
        result = lookup.lookup("profiles.default[2]");
        result = lookup.lookup("app.property");
        assertThat(result).as("Incorrect property value").isEqualTo("test");
    }

    @Test
    public void testSpringLookupWithDefaultInterpolator() {
        final MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty("app.property", "test");
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.putObject(Log4j2SpringBootLoggingSystem.ENVIRONMENT_KEY, env);
        final Interpolator lookup = new Interpolator();
        lookup.setConfiguration(context.getConfiguration());
        lookup.setLoggerContext(context);
        String result = lookup.lookup("spring:profiles.active");
        assertThat(result).as("Incorrect active profile").isEqualTo("test");

        result = lookup.lookup("spring:app.property");
        assertThat(result).as("Incorrect property value").isEqualTo("test");
    }
}
