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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Source}.
 */
class SourceTest {

    @Test
    void testEqualityFile() {
        assertEquals(new Source(new File("foo")), new Source(new File("foo")));
        assertEquals(new Source(new File("foo")), new Source(new File("./foo")));
        assertEquals(new Source(new File("foo.txt")), new Source(new File("./foo.txt")));
    }

    @Test
    void testEqualityPath() {
        assertEquals(new Source(Paths.get("foo")), new Source(Paths.get("foo")));
        assertEquals(new Source(Paths.get("foo")), new Source(Paths.get("./foo")));
        assertEquals(new Source(Paths.get("foo.txt")), new Source(Paths.get("./foo.txt")));
    }

    @Test
    @Disabled("File URI is broken.")
    void testEqualityURIFile() {
        assertEquals(
                new Source(Paths.get("foo").toUri()),
                new Source(Paths.get("./foo").toUri()));
    }

    @Test
    void testEqualityURIHttp() {
        assertEquals(
                new Source(URI.create("http://www.apache.org/index.html")),
                new Source(URI.create("http://www.apache.org/index.html")));
        assertEquals(
                new Source(URI.create("http://www.apache.org/")), new Source(URI.create("http://www.apache.org////")));
        assertEquals(
                new Source(URI.create("http://www.apache.org/")),
                new Source(URI.create("http://www.apache.org/./././.")));
    }

    @Test
    @Disabled
    void testEqualityURLFile() throws MalformedURLException {
        assertEquals(
                new Source(Paths.get("foo").toUri().toURL()),
                new Source(Paths.get("./foo").toUri().toURL()));
    }

    @Test
    @Disabled
    void testEqualityURLHttp() throws MalformedURLException {
        assertEquals(
                new Source(URI.create("http://www.apache.org/index.html").toURL()),
                new Source(URI.create("http://www.apache.org/index.html").toURL()));
        assertEquals(
                new Source(URI.create("http://www.apache.org").toURL()),
                new Source(URI.create("http://www.apache.org////").toURL()));
        assertEquals(
                new Source(URI.create("http://www.apache.org").toURL()),
                new Source(URI.create("http://www.apache.org/./././.").toURL()));
    }

    @Test
    @Disabled
    void testEqualityURLHttps() throws MalformedURLException {
        assertEquals(
                new Source(URI.create("https://www.apache.org/index.html").toURL()),
                new Source(URI.create("https://www.apache.org/index.html").toURL()));
        assertEquals(
                new Source(URI.create("https://www.apache.org").toURL()),
                new Source(URI.create("https://www.apache.org////").toURL()));
        assertEquals(
                new Source(URI.create("https://www.apache.org").toURL()),
                new Source(URI.create("https://www.apache.org/./././.").toURL()));
    }

    @Test
    void testFileConstructor() {
        final Path path = Paths.get("foo");
        final URI uri = path.toUri();
        final File file = path.toFile();
        final Source source = new Source(file);
        assertEquals(file, source.getFile());
        assertEquals(path, source.getFile().toPath());
        assertEquals(path, source.getPath());
        assertEquals(uri, source.getURI());
    }

    @Test
    void testPathStringConstructor() {
        final Path path = Paths.get("foo");
        final URI uri = path.toUri();
        final File file = path.toFile();
        final Source source = new Source(path);
        assertEquals(file, source.getFile());
        assertEquals(path, source.getFile().toPath());
        assertEquals(path, source.getPath());
        assertEquals(uri, source.getURI());
    }

    @Test
    void testPathURIFileConstructor() {
        final Path path = Paths.get(URI.create("file:///C:/foo"));
        final URI uri = path.toUri();
        final File file = path.toFile();
        final Source source = new Source(path);
        assertEquals(file, source.getFile());
        assertEquals(path, source.getFile().toPath());
        assertEquals(path, source.getPath());
        assertEquals(uri, source.getURI());
    }

    @Test
    void testURIConstructor() {
        final Path path = Paths.get("foo");
        final URI uri = path.toUri();
        final File file = path.toFile();
        final Source source = new Source(uri);
        assertEquals(file.getAbsoluteFile(), source.getFile());
        assertEquals(uri.toString(), source.getLocation());
        assertEquals(path.toAbsolutePath(), source.getPath());
    }

    @Test
    void testURIFileConstructor() {
        final URI uri = URI.create("file:///C:/foo");
        final Path path = Paths.get(uri);
        final File file = path.toFile();
        final Source source = new Source(uri);
        assertEquals(file.getAbsoluteFile(), source.getFile());
        assertEquals(uri.toString(), source.getLocation());
    }

    @Test
    void testURIHttpConstructor() {
        final URI uri = URI.create("http://www.apache.org");
        final Source source = new Source(uri);
        assertNull(source.getFile());
        assertEquals(uri.toString(), source.getLocation());
    }

    @Test
    void testURIHttpsConstructor() {
        final URI uri = URI.create("https://www.apache.org");
        final Source source = new Source(uri);
        assertNull(source.getFile());
        assertEquals(uri.toString(), source.getLocation());
    }

    @Test
    void testURLConstructor() throws MalformedURLException {
        final Path path = Paths.get("foo");
        final File file = path.toFile();
        final URI uri = path.toUri();
        final URL url = uri.toURL();
        final Source source = new Source(url);
        assertEquals(file.getAbsoluteFile(), source.getFile());
        assertEquals(url.toString(), source.getLocation());
        assertEquals(path.toAbsolutePath(), source.getPath());
    }
}
