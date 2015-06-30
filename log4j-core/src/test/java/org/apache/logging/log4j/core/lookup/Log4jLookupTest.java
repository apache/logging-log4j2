/*
 * Copyright 2015 Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.lookup;

import static org.apache.logging.log4j.core.lookup.Log4jLookup.KEY_CONFIG_LOCATION;
import static org.apache.logging.log4j.core.lookup.Log4jLookup.KEY_CONFIG_PARENT_LOCATION;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class Log4jLookupTest {

    private LoggerContext mockCtx = null;

    @Before
    public void setup() throws URISyntaxException {
        this.mockCtx = EasyMock.createMock(LoggerContext.class);
        expect(mockCtx.getConfigLocation()).andReturn(new URI("/a/b/c/d/e/log4j2.xml"));
        ContextAnchor.THREAD_CONTEXT.set(mockCtx);

        replay(mockCtx);
    }

    @After
    public void cleanup() {
        verify(mockCtx);

        ContextAnchor.THREAD_CONTEXT.set(null);
        this.mockCtx = null;
    }

    @Test
    public void lookupConfigLocation() {
        final StrLookup log4jLookup = new Log4jLookup();
        final String value = log4jLookup.lookup(KEY_CONFIG_LOCATION);
        assertEquals("/a/b/c/d/e/log4j2.xml", value);
    }

    @Test
    public void lookupConfigParentLocation() {
        final StrLookup log4jLookup = new Log4jLookup();
        final String value = log4jLookup.lookup(KEY_CONFIG_PARENT_LOCATION);
        assertEquals("/a/b/c/d/e", value);
    }
}
