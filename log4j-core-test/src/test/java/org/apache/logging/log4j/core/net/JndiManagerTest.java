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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JndiManager}.
 */
class JndiManagerTest {

    private static final String TRUE = "true";
    private static final AtomicInteger INITIAL_CONTEXT_CREATIONS = new AtomicInteger();
    private static final List<Context> CONTEXTS = new ArrayList<>();

    public static final class TestInitialContextFactory implements InitialContextFactory {

        @Override
        public Context getInitialContext(final Hashtable<?, ?> environment) {
            INITIAL_CONTEXT_CREATIONS.incrementAndGet();
            final Context context = mock(Context.class);
            CONTEXTS.add(context);
            return context;
        }
    }

    public static final class FailingInitialContextFactory implements InitialContextFactory {

        @Override
        public Context getInitialContext(final Hashtable<?, ?> environment) throws NamingException {
            throw new NamingException("test");
        }
    }

    @Test
    void testIsJndiContextSelectorEnabled() {
        assertFalse(JndiManager.isJndiContextSelectorEnabled());
        try {
            System.setProperty("log4j2.enableJndiContextSelector", TRUE);
            assertTrue(JndiManager.isJndiContextSelectorEnabled());
        } finally {
            System.clearProperty("log4j2.enableJndiContextSelector");
        }
    }

    @Test
    void testIsJndiEnabled() {
        assertFalse(JndiManager.isJndiEnabled());
        try {
            System.setProperty("log4j2.enableJndiJms", TRUE);
            assertTrue(JndiManager.isJndiEnabled());
        } finally {
            System.clearProperty("log4j2.enableJndiJms");
        }
    }

    @Test
    void testIsJndiJdbcEnabled() {
        assertFalse(JndiManager.isJndiJdbcEnabled());
        try {
            System.setProperty("log4j2.enableJndiJdbc", TRUE);
            assertTrue(JndiManager.isJndiJdbcEnabled());
        } finally {
            System.clearProperty("log4j2.enableJndiJdbc");
        }
    }

    @Test
    void testIsJndiJmsEnabled() {
        assertFalse(JndiManager.isJndiJmsEnabled());
        try {
            System.setProperty("log4j2.enableJndiJms", TRUE);
            assertTrue(JndiManager.isJndiJmsEnabled());
        } finally {
            System.clearProperty("log4j2.enableJndiJms");
        }
    }

    @Test
    void testIsJndiLookupEnabled() {
        assertFalse(JndiManager.isJndiLookupEnabled());
    }

    @Test
    void testJndiManagersAreNotSharedAcrossEnvironments() {
        System.setProperty("log4j2.enableJndiJms", TRUE);
        INITIAL_CONTEXT_CREATIONS.set(0);
        try {
            final Properties firstProperties = new Properties();
            firstProperties.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestInitialContextFactory.class.getName());
            firstProperties.setProperty(Context.PROVIDER_URL, "provider-one");

            final Properties secondProperties = new Properties();
            secondProperties.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestInitialContextFactory.class.getName());
            secondProperties.setProperty(Context.PROVIDER_URL, "provider-two");

            try (final JndiManager first = JndiManager.getJndiManager(firstProperties);
                    final JndiManager second = JndiManager.getJndiManager(secondProperties)) {
                assertNotSame(first, second);
                assertEquals(2, INITIAL_CONTEXT_CREATIONS.get());
            }
        } finally {
            System.clearProperty("log4j2.enableJndiJms");
        }
    }

    @Test
    void testDefaultManagersAreNotShared() {
        System.setProperty("log4j2.enableJndiJms", TRUE);
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestInitialContextFactory.class.getName());
        INITIAL_CONTEXT_CREATIONS.set(0);
        try {
            try (final JndiManager first = JndiManager.getDefaultManager();
                    final JndiManager second = JndiManager.getDefaultManager()) {
                assertNotSame(first, second);
                assertEquals(2, INITIAL_CONTEXT_CREATIONS.get());
            }
        } finally {
            System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
            System.clearProperty("log4j2.enableJndiJms");
        }
    }

    @Test
    void testNamingExceptionIsRethrown() {
        System.setProperty("log4j2.enableJndiJms", TRUE);
        try {
            final Properties properties = new Properties();
            properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, FailingInitialContextFactory.class.getName());
            assertThrows(IllegalStateException.class, () -> JndiManager.getJndiManager(properties));
        } finally {
            System.clearProperty("log4j2.enableJndiJms");
        }
    }

    @Test
    void testCloseClosesContext() throws Exception {
        System.setProperty("log4j2.enableJndiJms", TRUE);
        CONTEXTS.clear();
        try {
            final Properties properties = new Properties();
            properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestInitialContextFactory.class.getName());
            JndiManager.getJndiManager(properties).close();
            verify(CONTEXTS.get(0)).close();
        } finally {
            System.clearProperty("log4j2.enableJndiJms");
        }
    }

    @Test
    void testNoInstanceByDefault() {
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
