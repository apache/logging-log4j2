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
package org.apache.logging.log4j.core.config.xml;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class XmlSchemaTest {

    private static final String TARGET_NAMESPACE = "http://logging.apache.org/log4j/2.0/config";

    private static final List<String> IGNORE_CONFIGS = Arrays.asList( //
            "log4j2-arbiters.xml", // Arbiters violate XML schema as they can appear anywhere
            "log4j2-scriptArbiters.xml",
            "log4j2-selectArbiters.xml",
            "log4j-core-gctests/src/test/resources/gcFreeLogging.xml", // has 2 <Pattern> tags defined
            "legacy-plugins.xml", //
            "logback", // logback configs
            "log4j-xinclude", //
            "log4j12", // log4j 1.x configs
            "perf-CountingNoOpAppender.xml", // uses test-appender CountingNoOp
            "reconfiguration-deadlock.xml", // uses test-appender ReconfigurationDeadlockTestAppender
            "AsyncWaitStrategy", // uses AsyncWaitStrategyFactory (LOG4J2-3472)
            "XmlConfigurationSecurity.xml" // used for testing XML parser; shouldn't be parseable in secure settings
    );

    private static String capitalizeTags(final String xml) {
        final StringBuffer sb = new StringBuffer();
        final Matcher m = Pattern.compile("([<][/]?[a-z])").matcher(xml);
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase());
        }
        return m.appendTail(sb).toString();
    }

    private static String fixXml(String xml) {
        xml = StringUtils.replace(xml, "JSONLayout", "JsonLayout");
        xml = StringUtils.replace(xml, "HTMLLayout", "HtmlLayout");
        xml = StringUtils.replace(xml, "XMLLayout", "XmlLayout");
        xml = StringUtils.replace(xml, "appender-ref", "AppenderRef");
        xml = StringUtils.replace(xml, " onmatch=", " onMatch=");
        xml = StringUtils.replace(xml, " onMisMatch=", " onMismatch=");
        xml = StringUtils.replace(xml, " onmismatch=", " onMismatch=");
        xml = StringUtils.replace(xml, "<Marker ", "<MarkerFilter ");
        xml = StringUtils.replace(xml, " Value=", " value=");
        xml = capitalizeTags(xml);
        return xml;
    }

    @Test
    public void testXmlSchemaValidation() throws SAXException, IOException {
        final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Schema schema = factory.newSchema(new StreamSource(new File("src/main/resources/Log4j-config.xsd")));
        final Validator validator = schema.newValidator();

        final XMLFilterImpl namespaceAdder = new XMLFilterImpl(XMLReaderFactory.createXMLReader()) {
            @Override
            public void startElement(final String namespace, final String localName, final String qName,
                    final Attributes atts) throws SAXException {
                super.startElement(TARGET_NAMESPACE, localName, qName, atts);
            }
        };

        final MutableInt configs = new MutableInt();
        final List<Exception> exceptions = new ArrayList<>();

        try (final Stream<Path> testResources = Files.list(Paths.get("src", "test", "resources"))) {
            testResources
                    .filter(filePath -> {
                        final String fileName = filePath.getFileName().toString();
                        if (!fileName.endsWith(".xml"))
                            return false;
                        for (final String ignore : IGNORE_CONFIGS) {
                            if (fileName.contains(ignore))
                                return false;
                        }
                        return true;
                    }) //
                    .forEach(filePath -> {
                        System.out.println("Validating " + configs.incrementAndGet() + ". [" + filePath + "]...");
                        System.out.flush();

                        try {
                            final String xml = fixXml(Files.readString(filePath));
                            validator.validate(new SAXSource(namespaceAdder,
                                    new InputSource(new ByteArrayInputStream(xml.getBytes()))), null);
                        } catch (final Exception ex) {
                            exceptions.add(ex);
                        }
                    });
        }

        assertThat(exceptions).isEmpty();
    }
}
