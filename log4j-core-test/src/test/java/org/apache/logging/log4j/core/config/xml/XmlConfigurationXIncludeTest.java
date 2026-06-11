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
package org.apache.logging.log4j.core.config.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Unit tests for the XInclude handling of {@link XmlConfiguration#newDocumentBuilder(boolean)}.
 * <p>
 * These tests exercise the {@link DocumentBuilder} directly, so they are independent of the
 * {@code log4j2.configurationEnableXInclude} property plumbing covered by the integration tests in
 * {@code ConfigurationFactoryTest}.
 * </p>
 */
class XmlConfigurationXIncludeTest {

    private Document parse(final boolean enableXInclude) throws Exception {
        return parse("/log4j-xinclude.xml", enableXInclude);
    }

    private Document parse(final String resource, final boolean enableXInclude) throws Exception {
        final URL url = getClass().getResource(resource);
        final DocumentBuilder builder = XmlConfiguration.newDocumentBuilder(enableXInclude);
        final InputSource source = new InputSource(url.openStream());
        // Required so that relative `href` attributes can be resolved against the configuration location.
        source.setSystemId(url.toExternalForm());
        return builder.parse(source);
    }

    @Test
    void xInclude_enabled_resolves_includes() throws Exception {
        final Document document = parse(true);
        // The `xi:include` elements have been replaced by the content of the included files.
        assertEquals(0, document.getElementsByTagNameNS("*", "include").getLength(), "no `xi:include` should remain");
        assertEquals(1, document.getElementsByTagName("Console").getLength(), "Console appender from include");
        assertEquals(1, document.getElementsByTagName("File").getLength(), "File appender from include");
        assertEquals(1, document.getElementsByTagName("List").getLength(), "List appender from include");
        assertEquals(2, document.getElementsByTagName("Logger").getLength(), "Loggers from include");
    }

    @Test
    void xInclude_disabled_keeps_includes_unresolved() throws Exception {
        final Document document = parse(false);
        // Without XInclude support the `xi:include` elements are left untouched and nothing is included.
        assertEquals(2, document.getElementsByTagNameNS("*", "include").getLength(), "`xi:include` elements remain");
        assertEquals(0, document.getElementsByTagName("Console").getLength(), "no appenders should be included");
    }

    @Test
    void xInclude_resolves_classpath_scheme() throws Exception {
        // The custom resolver delegates to `ConfigurationSource`, which understands the `classpath:` URI scheme.
        final Document document = parse("/log4j-xinclude-classpath.xml", true);
        assertEquals(0, document.getElementsByTagNameNS("*", "include").getLength(), "no `xi:include` should remain");
        assertEquals(
                1, document.getElementsByTagName("Console").getLength(), "Console appender from classpath include");
        assertEquals(2, document.getElementsByTagName("Logger").getLength(), "Loggers from classpath include");
    }
}
