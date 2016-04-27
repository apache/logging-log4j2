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

package org.apache.logging.log4j.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.Assume;
import org.junit.Test;

public class NetUtilsTest {

    private static final boolean IS_WINDOWS = PropertiesUtil.getProperties().isOsWindows();

    @Test
    public void testToUriWithoutBackslashes() throws URISyntaxException {
        final String config = "file:///path/to/something/on/unix";
        final URI uri = NetUtils.toURI(config);

        assertNotNull("The URI should not be null.", uri);
        assertEquals("The URI is not correct.", "file:///path/to/something/on/unix", uri.toString());
    }

    @Test
    public void testToUriWindowsWithBackslashes() throws URISyntaxException {
        Assume.assumeTrue(IS_WINDOWS);
        final String config = "file:///D:\\path\\to\\something/on/windows";
        final URI uri = NetUtils.toURI(config);

        assertNotNull("The URI should not be null.", uri);
        assertEquals("The URI is not correct.", "file:///D:/path/to/something/on/windows", uri.toString());
    }

    @Test
    public void testToUriWindowsAbsolutePath() throws URISyntaxException {
        Assume.assumeTrue(IS_WINDOWS);
        final String config = "D:\\path\\to\\something\\on\\windows";
        final URI uri = NetUtils.toURI(config);

        assertNotNull("The URI should not be null.", uri);
        assertEquals("The URI is not correct.", "file:/D:/path/to/something/on/windows", uri.toString());
    }

}
