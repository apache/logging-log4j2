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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

public class XmlSchemaTest {

    private static final String TARGET_NAMESPACE = "http://logging.apache.org/log4j/2.0/config";

    private static final List<String> IGNORE_CONFIGS = Arrays.asList( //
            "log4j2-arbiters.xml", // Arbiters violate XML schema as they can appear anywhere
            "log4j2-environmentArbiters.xml",
            "log4j2-scriptArbiters.xml",
            "log4j2-systemPropertyArbiters.xml",
            "log4j2-selectArbiters.xml",
            "log4j-core-gctests/src/test/resources/gcFreeLogging.xml", // has 2 <Pattern> tags defined
            "legacy-plugins.xml", //
            "logback", // logback configs
            "log4j-xinclude", //
            "log4j12", // log4j 1.x configs
            "perf-CountingNoOpAppender.xml", // uses test-appender CountingNoOp
            "reconfiguration-deadlock.xml", // uses test-appender ReconfigurationDeadlockTestAppender
            "AsyncWaitStrategy", // uses AsyncWaitStrategyFactory (LOG4J2-3472)
            "XmlConfigurationSecurity.xml", // used for testing XML parser; shouldn't be parseable in secure settings
            "InvalidConfig.xml",
            "InvalidXML.xml");

    static Stream<Path> testXmlSchemaValidation() throws IOException {
        return Files.list(Paths.get("src", "test", "resources")).filter(filePath -> {
            final String fileName = filePath.getFileName().toString();
            if (!fileName.endsWith(".xml")) return false;
            for (final String ignore : IGNORE_CONFIGS) {
                if (fileName.contains(ignore)) return false;
            }
            return true;
        });
    }

    @ParameterizedTest
    @MethodSource
    public void testXmlSchemaValidation(final Path filePath) throws SAXException, IOException {
        final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Schema schema = factory.newSchema(new StreamSource(getClass().getResourceAsStream("/Log4j-config.xsd")));
        final Validator validator = schema.newValidator();

        final XMLFilterImpl namespaceAdder = new XMLFilterImpl(XMLReaderFactory.createXMLReader()) {
            @Override
            public void startElement(
                    final String namespace, final String localName, final String qName, final Attributes atts)
                    throws SAXException {
                super.startElement(TARGET_NAMESPACE, localName, qName, atts);
            }
        };

        final InputSource source = new InputSource(filePath.toAbsolutePath().toString());
        final SAXSource transformedSource = new SAXSource(namespaceAdder, source);
        assertDoesNotThrow(() -> {
            try {
                validator.validate(transformedSource);
            } catch (SAXParseException e) {
                // Wrap the exception to capture the location
                throw new RuntimeException(e.toString(), e);
            }
        });
    }
}
