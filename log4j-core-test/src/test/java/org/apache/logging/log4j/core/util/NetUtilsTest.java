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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.stream.Stream;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NetUtilsTest {

    /**
     * {@link NetUtils#toURI(String)} over a mix of URIs and file system paths.
     *
     * <p>Every input is exercised on every OS. Only the expected result differs.</p>
     */
    @ParameterizedTest
    @MethodSource
    void toUriConvertsLocationsToUris(final String input, final String expected) {
        assertThat(NetUtils.toURI(input)).hasToString(expected);
    }

    static Stream<Arguments> toUriConvertsLocationsToUris() {
        return Stream.of(
                // Genuine absolute URIs are returned verbatim on every platform.
                arguments("file:///path/to/something/on/unix", "file:///path/to/something/on/unix"),
                arguments("classpath:log4j2.xml", "classpath:log4j2.xml"),
                arguments("classloader:log4j2.xml", "classloader:log4j2.xml"),
                arguments("vfsfile:/some/path/log4j2.xml", "vfsfile:/some/path/log4j2.xml"),
                // Well-formed layered Apache Commons VFS URLs are valid (opaque) URIs, returned verbatim.
                arguments("zip:file:///path/a.zip!/dir/log4j2.xml", "zip:file:///path/a.zip!/dir/log4j2.xml"),
                arguments(
                        "tar:gz:http://example.com/a.tar.gz!/a.tar!/log4j2.xml",
                        "tar:gz:http://example.com/a.tar.gz!/a.tar!/log4j2.xml"),
                // Blanks are illegal in a URI: a hierarchical URL is rebuilt with them encoded, preserving user info,
                // host, port and query.
                arguments(
                        "http://example.com/a b/log4j2.xml?env=prod x",
                        "http://example.com/a%20b/log4j2.xml?env=prod%20x"),
                arguments(
                        "https://user@example.com:8443/a b/log4j2.xml",
                        "https://user@example.com:8443/a%20b/log4j2.xml"),
                // A relative path stays a scheme-less relative URI on every platform.
                arguments("relative/path/log4j2.xml", "relative/path/log4j2.xml"),
                // The remaining inputs are file system paths: absolute on one OS (resolved to a `file:` URI) and
                // relative on the other (a scheme-less URI, any drive-letter colon escaped to `%3A`).
                perOs("/path/without/spaces", "/path/without/spaces", "file:/path/without/spaces"),
                perOs(
                        "/ path / with / spaces",
                        "/%20path%20/%20with%20/%20spaces",
                        "file:/%20path%20/%20with%20/%20spaces"),
                perOs("C:/dir/log4j2.xml", "file:/C:/dir/log4j2.xml", "C%3A/dir/log4j2.xml"),
                perOs("C:\\dir\\log4j2.xml", "file:/C:/dir/log4j2.xml", "C%3A%5Cdir%5Clog4j2.xml"),
                perOs(
                        "D:\\path\\to\\something\\on\\windows",
                        "file:/D:/path/to/something/on/windows",
                        "D%3A%5Cpath%5Cto%5Csomething%5Con%5Cwindows"),
                perOs(
                        "file:///D:\\path\\to\\something/on/windows",
                        "file:///D:/path/to/something/on/windows",
                        "file:///D:%5Cpath%5Cto%5Csomething/on/windows"));
    }

    /**
     * Builds a case whose expected {@code toString()} differs between Windows and the other platforms.
     */
    private static Arguments perOs(final String input, final String onWindows, final String onUnix) {
        return arguments(input, SystemUtils.IS_OS_WINDOWS ? onWindows : onUnix);
    }

    @ParameterizedTest
    @MethodSource
    void toUrisSplitsOnComma(final String input, final String[] expected) {
        assertThat(NetUtils.toURIs(input)).extracting(URI::toString).containsExactly(expected);
    }

    static Stream<Arguments> toUrisSplitsOnComma() {
        return Stream.of(
                // A single location yields a single URI.
                arguments("classpath:log4j2.xml", new String[] {"classpath:log4j2.xml"}),
                // The scheme of the first location is propagated to the following ones, and blanks around the comma are
                // trimmed.
                arguments("classpath:first.xml, second.xml", new String[] {"classpath:first.xml", "classpath:second.xml"
                }),
                // Without a scheme each (relative) location is kept as-is.
                arguments("a/first.xml,b/second.xml", new String[] {"a/first.xml", "b/second.xml"}));
    }

    @Test
    void canonicalHostNameContainsDot() throws UnknownHostException {
        assumeThat(InetAddress.getLocalHost().getCanonicalHostName()).contains(".");
        // If this fails the host might be misconfigured.
        assertThat(NetUtils.getCanonicalLocalHostname()).contains(".");
    }
}
