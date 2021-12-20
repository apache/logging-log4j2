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

package org.apache.logging.log4j.core.net;

import static org.junit.Assert.assertFalse;

import java.util.Properties;

import org.apache.logging.log4j.test.JUnit5Bridge;
import org.junit.Test;

/**
 * Tests {@link JndiManager}.
 */
public class JndiManagerTest {

    @Test
    public void testIsJndiEnabled() {
        assertFalse(JndiManager.isJndiEnabled());
    }

    @Test
    public void testIsJndiContextSelectorEnabled() {
        assertFalse(JndiManager.isJndiContextSelectorEnabled());
    }

    @Test
    public void testIsJndiJmsEnabled() {
        assertFalse(JndiManager.isJndiJmsEnabled());
    }

    @Test
    public void testIsJndiLookupEnabled() {
        assertFalse(JndiManager.isJndiLookupEnabled());
    }

    @Test
    public void testNoInstanceByDefault() {
        JUnit5Bridge.assertThrows(IllegalStateException.class, new Runnable() {
            @Override
            public void run() {
                JndiManager.getDefaultManager();
            }
        });
        JUnit5Bridge.assertThrows(IllegalStateException.class, new Runnable() {
            @Override
            public void run() {
                JndiManager.getDefaultManager(null);
            }
        });
        JUnit5Bridge.assertThrows(IllegalStateException.class, new Runnable() {
            @Override
            public void run() {
                JndiManager.getDefaultManager("A");
            }
        });
        JUnit5Bridge.assertThrows(IllegalStateException.class, new Runnable() {
            @Override
            public void run() {
                JndiManager.getJndiManager(null);
            }
        });
        JUnit5Bridge.assertThrows(IllegalStateException.class, new Runnable() {
            @Override
            public void run() {
                JndiManager.getJndiManager(new Properties());
            }
        });
        JUnit5Bridge.assertThrows(IllegalStateException.class, new Runnable() {
            @Override
            public void run() {
                JndiManager.getJndiManager(null, null, null, null, null, null);
            }
        });
        JUnit5Bridge.assertThrows(IllegalStateException.class, new Runnable() {
            @Override
            public void run() {
                JndiManager.getJndiManager("A", "A", "A", "A", "A", new Properties());
            }
        });
    }

}
