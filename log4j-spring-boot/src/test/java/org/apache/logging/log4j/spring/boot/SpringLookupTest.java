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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.InterpolatorFactory;
import org.apache.logging.log4j.core.lookup.PropertiesLookup;
import org.junit.Test;
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
        assertNotNull("No active profiles", result);
        assertEquals("Incorrect active profile", "test", result);
        result = lookup.lookup("profiles.active[0]");
        assertNotNull("No active profiles", result);
        assertEquals("Incorrect active profile", "test", result);
        result = lookup.lookup("profiles.default");
        assertNotNull("No default profiles", result);
        assertEquals("Incorrect default profiles", "one,two", result);
        result = lookup.lookup("profiles.default[0]");
        assertNotNull("No default profiles", result);
        assertEquals("Incorrect default profiles", "one", result);
        result = lookup.lookup("profiles.default[2]");
        assertNull("Did not get index out of bounds", result);
        result = lookup.lookup("app.property");
        assertNotNull("Did not find property", result);
        assertEquals("Incorrect property value", "test", result);
    }

    @Test
    public void testSpringLookupWithDefaultInterpolator() {
        final MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty("app.property", "test");
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.putObject(Log4j2SpringBootLoggingSystem.ENVIRONMENT_KEY, env);
        final InterpolatorFactory interpolatorFactory =
                context.getInstanceFactory().getInstance(InterpolatorFactory.class);
        final Map<String, String> properties = new HashMap<>();
        final Interpolator lookup = interpolatorFactory.newInterpolator(new PropertiesLookup(properties));
        lookup.setLoggerContext(context);
        String result = lookup.lookup("spring:profiles.active");
        assertNotNull("No active profiles", result);
        assertEquals("Incorrect active profile", "test", result);

        result = lookup.lookup("spring:app.property");
        assertNotNull("Did not find property", result);
        assertEquals("Incorrect property value", "test", result);
    }
}
