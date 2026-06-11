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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Node;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Unit tests for the XInclude handling of {@link XmlConfiguration}.
 * <p>
 * The {@code parse}-based tests exercise the {@link DocumentBuilder} returned by
 * {@link XmlConfiguration#newDocumentBuilder(boolean)} directly, so they are independent of the
 * {@code log4j2.configurationEnableXInclude} property plumbing covered by the integration tests in
 * {@code ConfigurationFactoryTest}. {@link #fixupAttributesAreStripped()} drives {@link XmlConfiguration#constructHierarchy}
 * to verify that the {@code xml:base}/{@code xml:lang} attributes added by the XInclude fix-up features are not
 * exposed as Log4j configuration attributes.
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

    /**
     * The XInclude {@code fixup-base-uris} and {@code fixup-language} features (both enabled by default) add
     * {@code xml:base} and {@code xml:lang} attributes to the top-level included elements. Those belong to the
     * reserved XML namespace and must not be exposed as Log4j configuration attributes.
     */
    @Test
    void fixupAttributesAreStripped() throws Exception {
        // `log4j-xinclude-fixup.xml` carries `xml:lang` on the root and includes a `<Console>` appender, so the
        // included element receives both `xml:base` (from `fixup-base-uris`) and `xml:lang` (from `fixup-language`).
        final Element rootElement = parse("/log4j-xinclude-fixup.xml", true).getDocumentElement();

        final ConfigurationSource source =
                ConfigurationSource.fromResource("log4j-xinclude-fixup.xml", getClass().getClassLoader());
        final XmlConfiguration configuration = new XmlConfiguration(new LoggerContext("test"), source);
        // `constructHierarchy` resolves child elements to plugin types, so the plugins must be collected first.
        configuration.getPluginManager().collectPlugins();

        final Node root = new Node();
        configuration.constructHierarchy(root, rootElement);

        // Sanity check: the include was resolved and the `<Console>` node is present in the tree.
        assertThat(collectNodeNames(root, new ArrayList<>())).contains("Console");
        // None of the nodes carries an `xml:`-namespaced attribute.
        assertThat(collectXmlNamespaceAttributes(root, new ArrayList<>())).isEmpty();
    }

    private static List<String> collectNodeNames(final Node node, final List<String> names) {
        names.add(node.getName());
        node.getChildren().forEach(child -> collectNodeNames(child, names));
        return names;
    }

    private static List<String> collectXmlNamespaceAttributes(final Node node, final List<String> found) {
        node.getAttributes().keySet().stream().filter(key -> key.startsWith("xml:")).forEach(found::add);
        node.getChildren().forEach(child -> collectXmlNamespaceAttributes(child, found));
        return found;
    }
}
