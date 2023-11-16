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
package org.apache.logging.log4j.core.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JndiManager}.
 */
public class JndiManagerTest {

    private static final String TRUE = "true";

    @Test
    public void testIsJndiContextSelectorEnabled() {
        assertFalse(JndiManager.isJndiContextSelectorEnabled());
        try {
            System.setProperty("log4j2.enableJndiContextSelector", TRUE);
            assertTrue(JndiManager.isJndiContextSelectorEnabled());
        } finally {
            System.clearProperty("log4j2.enableJndiContextSelector");
        }
    }

    @Test
    public void testIsJndiEnabled() {
        assertFalse(JndiManager.isJndiEnabled());
        try {
            System.setProperty("log4j2.enableJndiJms", TRUE);
            assertTrue(JndiManager.isJndiEnabled());
        } finally {
            System.clearProperty("log4j2.enableJndiJms");
        }
    }

    @Test
    public void testIsJndiJdbcEnabled() {
        assertFalse(JndiManager.isJndiJdbcEnabled());
        try {
            System.setProperty("log4j2.enableJndiJdbc", TRUE);
            assertTrue(JndiManager.isJndiJdbcEnabled());
        } finally {
            System.clearProperty("log4j2.enableJndiJdbc");
        }
    }

    @Test
    public void testIsJndiJmsEnabled() {
        assertFalse(JndiManager.isJndiJmsEnabled());
        try {
            System.setProperty("log4j2.enableJndiJms", TRUE);
            assertTrue(JndiManager.isJndiJmsEnabled());
        } finally {
            System.clearProperty("log4j2.enableJndiJms");
        }
    }

    @Test
    public void testIsJndiLookupEnabled() {
        assertFalse(JndiManager.isJndiLookupEnabled());
    }

    @Test
    public void testNoInstanceByDefault() {
        assertThrows(IllegalStateException.class, () -> JndiManager.getDefaultManager());
        assertThrows(IllegalStateException.class, () -> JndiManager.getDefaultManager(null));
        assertThrows(IllegalStateException.class, () -> JndiManager.getDefaultManager("A"));
        assertThrows(IllegalStateException.class, () -> JndiManager.getJndiManager(null));
        assertThrows(IllegalStateException.class, () -> JndiManager.getJndiManager(new Properties()));
        assertThrows(IllegalStateException.class, () -> JndiManager.getJndiManager(null, null, null, null, null, null));
        assertThrows(
                IllegalStateException.class,
                () -> JndiManager.getJndiManager("A", "A", "A", "A", "A", new Properties()));
    }
}
