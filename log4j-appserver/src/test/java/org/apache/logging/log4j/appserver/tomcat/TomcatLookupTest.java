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
package org.apache.logging.log4j.appserver.tomcat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.catalina.loader.WebappClassLoader;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TomcatLookupTest {

    private static ClassLoader originalTccl;

    private static final String ENGINE_NAME = "Catalina";
    private static final String HOST_NAME = "localhost";
    private static final String CONTEXT_NAME = "/myapp";

    @BeforeAll
    public static void setupContextClassloader() {
        originalTccl = Thread.currentThread().getContextClassLoader();
        WebappClassLoader tccl = Mockito.mock(WebappClassLoader.class);
        Mockito.when(tccl.getServiceName()).thenReturn(ENGINE_NAME);
        Mockito.when(tccl.getHostName()).thenReturn(HOST_NAME);
        Mockito.when(tccl.getWebappName()).thenReturn(CONTEXT_NAME);
        Thread.currentThread().setContextClassLoader(tccl);
    }

    @AfterAll
    public static void clearContextClassloader() {
        Thread.currentThread().setContextClassLoader(originalTccl);
    }

    @Test
    public void lookupWorksProperly() {
        final StrLookup lookup = new TomcatLookup();
        assertEquals(ENGINE_NAME, lookup.lookup("classloader.serviceName"));
        assertEquals(ENGINE_NAME, lookup.lookup("catalina.engine.name"));
        assertEquals(HOST_NAME, lookup.lookup("classloader.hostName"));
        assertEquals(HOST_NAME, lookup.lookup("catalina.host.name"));
        assertEquals(CONTEXT_NAME, lookup.lookup("classloader.webappName"));
        assertEquals(CONTEXT_NAME, lookup.lookup("catalina.context.name"));
        assertEquals("org.apache.catalina.core.ContainerBase.[Catalina]", lookup.lookup("catalina.engine.logger"));
        assertEquals("org.apache.catalina.core.ContainerBase.[Catalina].[localhost]",
                lookup.lookup("catalina.host.logger"));
        assertEquals("org.apache.catalina.core.ContainerBase.[Catalina].[localhost].[/myapp]",
                lookup.lookup("catalina.context.logger"));
    }
}
