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
package org.apache.logging.log4j.internal.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * A SAX2-based XML reader.
 */
public final class XmlReader {

    private XmlReader() {}

    public static Element readXmlFileRootElement(final Path path, final String rootElementName) {
        try (final InputStream inputStream = new FileInputStream(path.toFile())) {
            final Document document = readXml(inputStream);
            final Element rootElement = document.getDocumentElement();
            if (!rootElementName.equals(rootElement.getNodeName())) {
                final String message = String.format(
                        "was expecting root element to be called `%s`, found: `%s`",
                        rootElementName, rootElement.getNodeName());
                throw new IllegalArgumentException(message);
            }
            return rootElement;
        } catch (final Exception error) {
            final String message = String.format(
                    "XML read failure for file `%s` and root element `%s`", path, rootElementName);
            throw new RuntimeException(message, error);
        }
    }

    private static Document readXml(final InputStream inputStream) throws Exception {
        final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        final SAXParser parser = parserFactory.newSAXParser();
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        final Document document = documentBuilder.newDocument();
        PositionalSaxEventHandler handler = new PositionalSaxEventHandler(document);
        parser.parse(inputStream, handler);
        return document;
    }

    public static RuntimeException failureAtXmlNode(
            final Node node,
            final String messageFormat,
            final Object... messageArgs) {
        return failureAtXmlNode(null, node, messageFormat, messageArgs);
    }

    public static RuntimeException failureAtXmlNode(
            final Throwable cause,
            final Node node,
            final String messageFormat,
            final Object... messageArgs) {
        final Object lineNumber = node.getUserData("lineNumber");
        final String messagePrefix = String.format("[line %s] ", lineNumber);
        final String message = String.format(messagePrefix + messageFormat, messageArgs);
        return new IllegalArgumentException(message, cause);
    }

}
