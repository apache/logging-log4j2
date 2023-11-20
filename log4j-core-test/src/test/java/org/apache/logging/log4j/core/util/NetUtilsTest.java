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
package org.apache.logging.log4j.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class NetUtilsTest {

    @Test
    public void testToUriWithoutBackslashes() {
        final String config = "file:///path/to/something/on/unix";
        URI uri = NetUtils.toURI(config);

        assertNotNull(uri, "The URI should not be null.");
        assertEquals("file:///path/to/something/on/unix", uri.toString(), "The URI is not correct.");

        final String properUriPath = "/path/without/spaces";
        uri = NetUtils.toURI(properUriPath);

        assertNotNull(uri, "The URI should not be null.");
        assertEquals(properUriPath, uri.toString(), "The URI is not correct.");
    }

    @Test
    public void testToUriUnixWithSpaces() {
        final String pathWithSpaces = "/ path / with / spaces";
        final URI uri = NetUtils.toURI(pathWithSpaces);

        assertNotNull(uri, "The URI should not be null.");
        assertEquals(new File(pathWithSpaces).toURI().toString(), uri.toString(), "The URI is not correct.");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testToUriWindowsWithBackslashes() {
        final String config = "file:///D:\\path\\to\\something/on/windows";
        final URI uri = NetUtils.toURI(config);

        assertNotNull(uri, "The URI should not be null.");
        assertEquals("file:///D:/path/to/something/on/windows", uri.toString(), "The URI is not correct.");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testToUriWindowsAbsolutePath() {
        final String config = "D:\\path\\to\\something\\on\\windows";
        final URI uri = NetUtils.toURI(config);

        assertNotNull(uri, "The URI should not be null.");
        assertEquals("file:/D:/path/to/something/on/windows", uri.toString(), "The URI is not correct.");
    }
}
