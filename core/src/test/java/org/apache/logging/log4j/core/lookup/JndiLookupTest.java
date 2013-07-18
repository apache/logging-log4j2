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
package org.apache.logging.log4j.core.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockejb.jndi.MockContextFactory;

/**
 * JndiLookupTest
 */
public class JndiLookupTest {

    private static final String TEST_CONTEXT_RESOURCE_NAME = "logging/context-name";
    private static final String TEST_CONTEXT_NAME = "app-1";

    @Before
    public void before() throws NamingException {
        MockContextFactory.setAsInitial();
        Context context = new InitialContext();
        context.bind(JndiLookup.CONTAINER_JNDI_RESOURCE_PATH_PREFIX + TEST_CONTEXT_RESOURCE_NAME, TEST_CONTEXT_NAME);
    }

    @After
    public void after() {
        MockContextFactory.revertSetAsInitial();
    }

    @Test
    public void testLookup() {
        final StrLookup lookup = new JndiLookup();

        String contextName = lookup.lookup(TEST_CONTEXT_RESOURCE_NAME);
        assertEquals(TEST_CONTEXT_NAME, contextName);

        contextName = lookup.lookup(JndiLookup.CONTAINER_JNDI_RESOURCE_PATH_PREFIX + TEST_CONTEXT_RESOURCE_NAME);
        assertEquals(TEST_CONTEXT_NAME, contextName);

        String nonExistingResource = lookup.lookup("logging/non-existing-resource");
        assertNull(nonExistingResource);
    }
}
