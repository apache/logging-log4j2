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
package org.apache.logging.log4j.spring.boot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test SpringLookup.
 */
public class SpringLookupTest {

    @Test
    public void testLookup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setDefaultProfiles("one", "two");
        env.setProperty("app.property", "test");
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.putObject(Log4j2CloudConfigLoggingSystem.ENVIRONMENT_KEY, env);
        SpringLookup lookup = new SpringLookup();
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
}
