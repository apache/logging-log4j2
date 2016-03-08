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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationAware;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.apache.logging.log4j.core.lookup.Log4jLookup.*;

import static org.easymock.EasyMock.*;

import static org.junit.Assert.*;

/**
 *
 */
public class Log4jLookupWithSpacesTest {

    private final static File EXPECT = new File(System.getProperty("user.home"), "/a a/b b/c c/d d/e e/log4j2 file.xml");
    private LoggerContext mockCtx = null;
    private Configuration config = null;
    private ConfigurationSource configSrc = null;

    @Before
    public void setup() throws URISyntaxException, MalformedURLException {
        this.mockCtx = EasyMock.createMock(LoggerContext.class);
        ContextAnchor.THREAD_CONTEXT.set(mockCtx);

        this.config = EasyMock.createMock(Configuration.class);

        this.configSrc = EasyMock.createMock(ConfigurationSource.class);
        expect(config.getConfigurationSource()).andReturn(configSrc);
        expect(configSrc.getFile()).andReturn(EXPECT);

        replay(mockCtx, config, configSrc);
    }

    @After
    public void cleanup() {
        verify(mockCtx, config, configSrc);

        ContextAnchor.THREAD_CONTEXT.set(null);
        this.mockCtx = null;
    }

    @Test
    public void lookupConfigLocation_withSpaces() {
        final StrLookup log4jLookup = new Log4jLookup();
        ((ConfigurationAware) log4jLookup).setConfiguration(config);
        final String value = log4jLookup.lookup(KEY_CONFIG_LOCATION);
        assertEquals(
                new File(System.getProperty("user.home"), "/a a/b b/c c/d d/e e/log4j2 file.xml").getAbsolutePath(),
                value);
    }

    @Test
    public void lookupConfigParentLocation_withSpaces() {
        final StrLookup log4jLookup = new Log4jLookup();
        ((ConfigurationAware) log4jLookup).setConfiguration(config);
        final String value = log4jLookup.lookup(KEY_CONFIG_PARENT_LOCATION);
        assertEquals(new File(System.getProperty("user.home"), "/a a/b b/c c/d d/e e").getAbsolutePath(), value);
    }
}
