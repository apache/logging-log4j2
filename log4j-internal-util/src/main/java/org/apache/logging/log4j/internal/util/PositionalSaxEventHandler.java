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
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Stack;

/**
 * A SAX2 event handler adding the associated line number to each emitted nodes' user data.
 * <p>
 * The added node user data is keyed with {@code lineNumber}.
 * </p>
 */
final class PositionalSaxEventHandler extends DefaultHandler {

    private final Stack<Element> elementStack = new Stack<>();

    private final StringBuilder textBuffer = new StringBuilder();

    private final Document document;

    private Locator locator;

    PositionalSaxEventHandler(final Document document) {
        this.document = document;
    }

    @Override
    public void setDocumentLocator(final Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startElement(
            final String uri,
            final String localName,
            final String qName,
            final Attributes attributes) {
        addTextIfNeeded();
        final Element element = document.createElement(qName);
        for (int attributeIndex = 0; attributeIndex < attributes.getLength(); attributeIndex++) {
            final String attributeQName = attributes.getQName(attributeIndex);
            final String attributeValue = attributes.getValue(attributeIndex);
            element.setAttribute(attributeQName, attributeValue);
        }
        element.setUserData("lineNumber", String.valueOf(locator.getLineNumber()), null);
        elementStack.push(element);
    }

    @Override
    public void endElement(
            final String uri,
            final String localName,
            final String qName) {
        addTextIfNeeded();
        final Element closedElement = elementStack.pop();
        final boolean rootElement = elementStack.isEmpty();
        if (rootElement) {
            document.appendChild(closedElement);
        } else {
            final Element parentElement = elementStack.peek();
            parentElement.appendChild(closedElement);
        }
    }

    @Override
    public void characters(final char[] buffer, final int start, final int length) {
        textBuffer.append(buffer, start, length);
    }

    /**
     * Outputs text accumulated under the current node.
     */
    private void addTextIfNeeded() {
        if (textBuffer.length() > 0) {
            final Element element = elementStack.peek();
            final Node textNode = document.createTextNode(textBuffer.toString());
            element.appendChild(textNode);
            textBuffer.delete(0, textBuffer.length());
        }
    }

}
