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
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.lookup;

import static org.apache.logging.log4j.core.lookup.Log4jLookup.KEY_CONFIG_LOCATION;
import static org.apache.logging.log4j.core.lookup.Log4jLookup.KEY_CONFIG_PARENT_LOCATION;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class Log4jLookupTest {

    private final static File EXPECT = new File(System.getProperty("user.home"), "/a/b/c/d/e/log4j2.xml");
    private LoggerContext mockCtx = null;

    @Before
    public void setup() throws URISyntaxException {
        this.mockCtx = EasyMock.createMock(LoggerContext.class);
        ContextAnchor.THREAD_CONTEXT.set(mockCtx);

        final Configuration config = EasyMock.createMock(Configuration.class);
        expect(mockCtx.getConfiguration()).andReturn(config);
        
        final ConfigurationSource configSrc = EasyMock.createMock(ConfigurationSource.class);
        expect(config.getConfigurationSource()).andReturn(configSrc);
        expect(configSrc.getFile()).andReturn(EXPECT);

        replay(mockCtx);
        replay(config);
        replay(configSrc);
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
        assertEquals(EXPECT.getAbsolutePath(), value);
    }

    @Test
    public void lookupConfigParentLocation() {
        final StrLookup log4jLookup = new Log4jLookup();
        final String value = log4jLookup.lookup(KEY_CONFIG_PARENT_LOCATION);
        assertEquals(EXPECT.getParentFile().getAbsolutePath(), value);
    }
}
