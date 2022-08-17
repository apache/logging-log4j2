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

package org.apache.logging.log4j.jndi;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JndiManager}.
 */
public class JndiManagerTest {

    @Test
    public void testIsJndiEnabled() {
        Assertions.assertFalse(JndiManager.isJndiEnabled());
    }

    @Test
    public void testIsJndiContextSelectorEnabled() {
        Assertions.assertFalse(JndiManager.isJndiContextSelectorEnabled());
    }

    @Test
    public void testIsJndiJmsEnabled() {
        Assertions.assertFalse(JndiManager.isJndiJmsEnabled());
    }

    @Test
    public void testIsJndiLookupEnabled() {
        Assertions.assertFalse(JndiManager.isJndiLookupEnabled());
    }

    @Test
    public void testNoInstanceByDefault() {
        assertThrows(IllegalStateException.class, () -> JndiManager.getDefaultManager());
        assertThrows(IllegalStateException.class, () -> JndiManager.getDefaultManager(null));
        assertThrows(IllegalStateException.class, () -> JndiManager.getDefaultManager("A"));
        assertThrows(IllegalStateException.class, () -> JndiManager.getJndiManager(null));
        assertThrows(IllegalStateException.class, () -> JndiManager.getJndiManager(new Properties()));
        assertThrows(IllegalStateException.class, () -> JndiManager.getJndiManager(null, null, null, null, null, null));
        assertThrows(IllegalStateException.class, () -> JndiManager.getJndiManager("A", "A", "A", "A", "A", new Properties()));
    }


}
