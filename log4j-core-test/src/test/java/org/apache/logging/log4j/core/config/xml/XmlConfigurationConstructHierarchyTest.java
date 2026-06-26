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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.StringReader;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Unit tests for {@link XmlConfiguration#constructHierarchy(Node, Element, PluginManager, boolean)}.
 * <p>
 * These exercise the DOM-to-{@link Node} conversion in isolation: the {@link PluginManager} is mocked, so the test
 * decides which element names resolve to a plugin without pulling in the real plugin registry or a running
 * {@code LoggerContext}.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class XmlConfigurationConstructHierarchyTest {

    private static final String INCLUDED_URI = resourceUri("included.xml");
    private static final String FRAGMENT_URI = resourceUri("included-fragment.xml");
    private static final String WITH_ID_URI = resourceUri("included-with-id.xml");
    private static final String NESTED_URI = resourceUri("nested.xml");

    @Mock
    private PluginManager pluginManager;

    private static String resourceUri(final String name) {
        return Objects.requireNonNull(XmlConfigurationConstructHierarchyTest.class.getResource(
                        "/XmlConfigurationConstructHierarchyTest/" + name))
                .toExternalForm();
    }

    /** Parses an inline XML snippet into its root {@link Element}, configuring the parser exactly as production does. */
    private static Element parse(final String xml) throws Exception {
        return parse(xml, false);
    }

    private static Element parse(final String xml, final boolean enableXInclude) throws Exception {
        final DocumentBuilder builder = XmlConfiguration.newDocumentBuilder(enableXInclude);
        return builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
    }

    /** Builds an {@code <Appenders>} document with a single {@code xi:include} of {@code href}, optionally pointed. */
    private static String including(final String href, final String xpointer) {
        final String pointer = xpointer == null ? "" : " xpointer=\"" + xpointer + "\"";
        return "<Appenders xmlns:xi=\"http://www.w3.org/2001/XInclude\"><xi:include href=\"" + href + "\"" + pointer
                + "/></Appenders>";
    }

    @SuppressWarnings("unchecked")
    private void givenPlugin(final String name) {
        // `lenient`: `constructHierarchy` also looks up names that are intentionally left unresolved (returning
        // `null`), which strict stubbing would otherwise flag as an argument mismatch.
        lenient().when(pluginManager.getPluginType(name)).thenReturn(mock(PluginType.class));
    }

    /** A child element that resolves to a plugin becomes a child {@link Node} carrying that plugin type. */
    @Test
    void resolvedElementBecomesChildNode() throws Exception {
        givenPlugin("Console");
        final Element element = parse("<Appenders><Console name=\"STDOUT\"/></Appenders>");

        final Node root = new Node();
        XmlConfiguration.constructHierarchy(root, element, pluginManager, false);

        assertThat(root.getChildren()).singleElement().satisfies(child -> {
            assertThat(child.getName()).isEqualTo("Console");
            assertThat(child.getType()).isNotNull();
            assertThat(child.getAttributes()).containsEntry("name", "STDOUT");
        });
    }

    /** A leaf element with no plugin type and a text value is folded into its parent as an attribute. */
    @Test
    void unresolvedLeafElementBecomesAttribute() throws Exception {
        // `Pattern` has no plugin type, so `<PatternLayout>` absorbs it as a `Pattern` attribute.
        final Element element = parse("<PatternLayout><Pattern>%m%n</Pattern></PatternLayout>");

        final Node patternLayout = new Node();
        XmlConfiguration.constructHierarchy(patternLayout, element, pluginManager, false);

        assertThat(patternLayout.getChildren()).isEmpty();
        assertThat(patternLayout.getAttributes()).containsEntry("Pattern", "%m%n");
    }

    /** An element with no plugin type but with children cannot be folded: it is dropped and an error is logged. */
    @Test
    @UsingStatusListener
    void unresolvedElementWithChildrenIsDropped(final ListStatusListener listener) throws Exception {
        givenPlugin("Console");
        final Element element = parse("<Appenders><Unknown><Console/></Unknown></Appenders>");

        final Node appenders = new Node();
        XmlConfiguration.constructHierarchy(appenders, element, pluginManager, false);

        // The `Unknown` element (and the `Console` nested in it) are not added to the tree.
        assertThat(appenders.getChildren()).isEmpty();
        assertThat(listener.findStatusData(Level.ERROR))
                .anyMatch(data -> data.getMessage().getFormattedMessage().contains("Error processing element Unknown"));
    }

    /** The text content of a resolved element becomes the node value. */
    @Test
    void textContentBecomesNodeValue() throws Exception {
        givenPlugin("Property");
        final Element element =
                parse("<Properties><Property name=\"filename\">target/test.log</Property></Properties>");

        final Node properties = new Node();
        XmlConfiguration.constructHierarchy(properties, element, pluginManager, false);

        assertThat(properties.getChildren()).singleElement().satisfies(child -> {
            assertThat(child.getName()).isEqualTo("Property");
            assertThat(child.getValue()).isEqualTo("target/test.log");
            assertThat(child.getAttributes()).containsEntry("name", "filename");
        });
    }

    /** In strict mode the plugin name comes from the {@code type} attribute, which is consumed in the process. */
    @Test
    void strictModeResolvesPluginFromTypeAttribute() throws Exception {
        givenPlugin("Console");
        final Element element = parse("<Appenders><Appender type=\"Console\" name=\"STDOUT\"/></Appenders>");

        final Node appenders = new Node();
        XmlConfiguration.constructHierarchy(appenders, element, pluginManager, true);

        assertThat(appenders.getChildren()).singleElement().satisfies(child -> {
            assertThat(child.getName()).isEqualTo("Console");
            // The `type` attribute is consumed by the plugin-name lookup and not exposed as a Log4j attribute.
            assertThat(child.getAttributes()).containsEntry("name", "STDOUT").doesNotContainKey("type");
        });
    }

    /** Without strict mode the tag name is the plugin name and {@code type} stays an ordinary attribute. */
    @Test
    void nonStrictModeResolvesPluginFromTagName() throws Exception {
        // With the tag name used as the plugin name, `Console` (the `type` attribute) is never looked up.
        givenPlugin("Appender");
        final Element element = parse("<Appenders><Appender type=\"Console\" name=\"STDOUT\"/></Appenders>");

        final Node appenders = new Node();
        XmlConfiguration.constructHierarchy(appenders, element, pluginManager, false);

        assertThat(appenders.getChildren()).singleElement().satisfies(child -> {
            assertThat(child.getName()).isEqualTo("Appender");
            assertThat(child.getAttributes()).containsEntry("type", "Console").containsEntry("name", "STDOUT");
        });
    }

    /** An {@code xml:base} attribute (as added by the XInclude {@code fixup-base-uris} feature) is not exposed. */
    @Test
    void xmlBaseAttributeIsStripped() throws Exception {
        givenPlugin("Console");
        final Element element = parse("<Appenders><Console xml:base=\"urn:somewhere\" name=\"STDOUT\"/></Appenders>");

        final Node appenders = new Node();
        XmlConfiguration.constructHierarchy(appenders, element, pluginManager, false);

        assertThat(appenders.getChildren()).singleElement().satisfies(child -> assertThat(child.getAttributes())
                .containsEntry("name", "STDOUT")
                .doesNotContainKey("xml:base"));
    }

    /** An {@code xml:lang} attribute (as added by the XInclude {@code fixup-language} feature) is not exposed. */
    @Test
    void xmlLangAttributeIsStripped() throws Exception {
        givenPlugin("Console");
        final Element element = parse("<Appenders><Console xml:lang=\"en\" name=\"STDOUT\"/></Appenders>");

        final Node appenders = new Node();
        XmlConfiguration.constructHierarchy(appenders, element, pluginManager, false);

        assertThat(appenders.getChildren()).singleElement().satisfies(child -> assertThat(child.getAttributes())
                .containsEntry("name", "STDOUT")
                .doesNotContainKey("xml:lang"));
    }

    /**
     * Checks the relevance of Log4j's attribute stripping
     * 
     * <p>The XInclude {@code fixup-base-uris} and {@code fixup-language} features default to {@code true}, so a normal
     * XInclude parse really does add {@code xml:base} and {@code xml:lang} to the included element.</p>
     */
    @Test
    void fixupAttributesAreProducedByDefault() throws Exception {
        // `xml:lang` on the parent gives `fixup-language` a value to reconcile on the included `<Console>`.
        final String document = "<Appenders xmlns:xi=\"http://www.w3.org/2001/XInclude\" xml:lang=\"en\">"
                + "<xi:include href=\"" + INCLUDED_URI + "\"/></Appenders>";
        final Element console =
                (Element) parse(document, true).getElementsByTagName("Console").item(0);

        assertThat(console.hasAttributeNS(XMLConstants.XML_NS_URI, "base")).isTrue();
        assertThat(console.hasAttributeNS(XMLConstants.XML_NS_URI, "lang")).isTrue();
    }

    /** With XInclude enabled, an {@code xi:include} is resolved and its content spliced into the tree. */
    @Test
    void xIncludeIsHonored() throws Exception {
        givenPlugin("Console");
        final Element element = parse(including(INCLUDED_URI, null), true);

        final Node appenders = new Node();
        XmlConfiguration.constructHierarchy(appenders, element, pluginManager, false);

        // The `<Console>` from `included.xml` has replaced the `<xi:include>` element.
        assertThat(appenders.getChildren()).singleElement().satisfies(child -> {
            assertThat(child.getName()).isEqualTo("Console");
            assertThat(child.getAttributes()).containsEntry("name", "STDOUT");
        });
    }

    /** A positional {@code xpointer} ({@code element(/1/2)}) selects a single element by its position. */
    @Test
    void xIncludeResolvesPositionalPointer() throws Exception {
        givenPlugin("Console");
        // `element(/1/2)`: the second child element of the document root (`<Fragment>`).
        final Element element = parse(including(FRAGMENT_URI, "element(/1/2)"), true);

        final Node appenders = new Node();
        XmlConfiguration.constructHierarchy(appenders, element, pluginManager, false);

        assertThat(appenders.getChildren()).singleElement().satisfies(child -> {
            assertThat(child.getName()).isEqualTo("Console");
            assertThat(child.getAttributes()).containsEntry("name", "SECOND");
        });
    }

    /**
     * A shorthand {@code xpointer} selects an element by an {@code ID}-typed attribute.
     *
     * <p>The {@code name} attribute is typed {@code ID} through the included document's internal DTD subset,
     * which Log4j's hardening leaves enabled (only the <em>external</em> subset is disabled).</p>
     */
    @Test
    void xIncludeResolvesIdPointer() throws Exception {
        givenPlugin("Console");
        final Element element = parse(including(WITH_ID_URI, "OTHER"), true);

        final Node appenders = new Node();
        XmlConfiguration.constructHierarchy(appenders, element, pluginManager, false);

        assertThat(appenders.getChildren()).singleElement().satisfies(child -> {
            assertThat(child.getName()).isEqualTo("Console");
            assertThat(child.getAttributes()).containsEntry("name", "OTHER");
        });
    }

    /**
     * XInclude resolution is recursive: an included document may itself contain an {@code xi:include}. The inner
     * {@code href} is relative and must resolve against the <em>included</em> document's own location, not the
     * top-level configuration's.
     */
    @Test
    void xIncludeResolvesNestedIncludes() throws Exception {
        givenPlugin("Appenders");
        givenPlugin("Console");
        // `nested.xml` (an `<Appenders>` fragment) is included by absolute URI and itself includes `included.xml`
        // (a `<Console>`) by a relative href.
        final String document = "<Configuration xmlns:xi=\"http://www.w3.org/2001/XInclude\">"
                + "<xi:include href=\"" + NESTED_URI + "\"/></Configuration>";
        final Element element = parse(document, true);

        final Node configuration = new Node();
        XmlConfiguration.constructHierarchy(configuration, element, pluginManager, false);

        assertThat(configuration.getChildren()).singleElement().satisfies(appenders -> {
            assertThat(appenders.getName()).isEqualTo("Appenders");
            assertThat(appenders.getChildren()).singleElement().satisfies(console -> {
                assertThat(console.getName()).isEqualTo("Console");
                assertThat(console.getAttributes()).containsEntry("name", "STDOUT");
            });
        });
    }
}
