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
package org.apache.logging.log4j.core.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.fasterxml.jackson.dataformat.xml.XmlNameProcessor;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.ri.Stax2WriterAdapter;
import org.codehaus.stax2.util.StreamWriter2Delegate;

/**
 * A Jackson XML {@link ObjectMapper} initialized for Log4j.
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
public class Log4jXmlObjectMapper extends XmlMapper {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new instance using the {@link Log4jXmlModule}.
     */
    public Log4jXmlObjectMapper() {
        this(true, false);
    }

    /**
     * Create a new instance using the {@link Log4jXmlModule}.
     */
    public Log4jXmlObjectMapper(final boolean includeStacktrace, final boolean stacktraceAsString) {
        super(new SanitizingXmlFactory(), new Log4jXmlModule(includeStacktrace, stacktraceAsString));
        this.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    /**
     * Writer that sanitizes text to be valid XML 1.0 by replacing disallowed code points with the replacement character (U+FFFD).
     */
    private static final class SanitizingWriter extends StreamWriter2Delegate {

        private static final char REPLACEMENT_CHAR = '\uFFFD';

        SanitizingWriter(final XMLStreamWriter2 delegate) {
            super(delegate);
            setParent(delegate);
        }

        @Override
        public void writeAttribute(final String localName, final String value) throws XMLStreamException {
            super.writeAttribute(localName, sanitizeXml10(value));
        }

        @Override
        public void writeAttribute(final String namespaceURI, final String localName, final String value)
                throws XMLStreamException {
            super.writeAttribute(namespaceURI, localName, sanitizeXml10(value));
        }

        @Override
        public void writeAttribute(
                final String prefix, final String namespaceURI, final String localName, final String value)
                throws XMLStreamException {
            super.writeAttribute(prefix, namespaceURI, localName, sanitizeXml10(value));
        }

        @Override
        public void writeCData(String text) throws XMLStreamException {
            super.writeCData(sanitizeXml10(text));
        }

        @Override
        public void writeCData(char[] text, int start, int len) throws XMLStreamException {
            super.writeCData(sanitizeXml10(text, start, len));
        }

        @Override
        public void writeCharacters(final String text) throws XMLStreamException {
            super.writeCharacters(sanitizeXml10(text));
        }

        @Override
        public void writeCharacters(final char[] text, final int start, final int len) throws XMLStreamException {
            super.writeCharacters(sanitizeXml10(text, start, len));
        }

        @Override
        public void writeComment(String text) throws XMLStreamException {
            super.writeComment(sanitizeXml10(text));
        }

        private static String sanitizeXml10(final String input) {
            if (input == null) {
                return null;
            }
            final int length = input.length();
            // Only create a new string if we find an invalid code point.
            // In the common case, this should avoid unnecessary allocations.
            for (int i = 0; i < length; ) {
                final int cp = input.codePointAt(i);
                if (!isValidXml10(cp)) {
                    final StringBuilder out = new StringBuilder(length);
                    out.append(input, 0, i);
                    appendSanitized(input, i, length, out);
                    return out.toString();
                }
                i += Character.charCount(cp);
            }
            return input;
        }

        private static String sanitizeXml10(final char[] input, final int start, final int len) {
            return sanitizeXml10(new String(input, start, len));
        }

        private static void appendSanitized(final String input, int i, final int length, final StringBuilder out) {
            while (i < length) {
                final int cp = input.codePointAt(i);
                out.appendCodePoint(isValidXml10(cp) ? cp : REPLACEMENT_CHAR);
                i += Character.charCount(cp);
            }
        }

        /**
         * Checks if a code point is valid
         *
         * @param codePoint a code point between {@code 0} and {@link Character#MAX_CODE_POINT}
         * @return {@code true} if it is a valid XML 1.0 code point
         */
        private static boolean isValidXml10(final int codePoint) {
            assert codePoint >= 0 && codePoint <= Character.MAX_CODE_POINT;
            // XML 1.0 valid characters (Fifth Edition):
            //   #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]

            // [#x20–#xD7FF] (placed early as a fast path for the most common case)
            return (codePoint >= ' ' && codePoint < Character.MIN_SURROGATE)
                    // #x9
                    || codePoint == '\t'
                    // #xA
                    || codePoint == '\n'
                    // #xD
                    || codePoint == '\r'
                    // [#xE000-#xFFFD]
                    || (codePoint > Character.MAX_SURROGATE && codePoint <= 0xFFFD)
                    // [#x10000-#x10FFFF]
                    || codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT;
        }
    }

    /**
     * Factory that creates {@link SanitizingWriter} instances to ensure that all text written to the XML output is valid XML 1.0.
     */
    private static final class SanitizingXmlFactory extends XmlFactory {

        private static final long serialVersionUID = 1L;

        public SanitizingXmlFactory() {
            super();
        }

        private SanitizingXmlFactory(
                ObjectCodec oc,
                int xpFeatures,
                int xgFeatures,
                XMLInputFactory xmlIn,
                XMLOutputFactory xmlOut,
                String nameForTextElem,
                XmlNameProcessor nameProcessor) {
            super(oc, xpFeatures, xgFeatures, xmlIn, xmlOut, nameForTextElem, nameProcessor);
        }

        @Override
        protected XMLStreamWriter _createXmlWriter(final IOContext ctxt, final Writer w) throws IOException {
            return new SanitizingWriter(Stax2WriterAdapter.wrapIfNecessary(super._createXmlWriter(ctxt, w)));
        }

        @Override
        protected XMLStreamWriter _createXmlWriter(final IOContext ctxt, final OutputStream out) throws IOException {
            return new SanitizingWriter(Stax2WriterAdapter.wrapIfNecessary(super._createXmlWriter(ctxt, out)));
        }

        @Override
        public XmlFactory copy() {
            _checkInvalidCopy(SanitizingXmlFactory.class);
            return new SanitizingXmlFactory(
                    _objectCodec,
                    _xmlParserFeatures,
                    _xmlGeneratorFeatures,
                    _xmlInputFactory,
                    _xmlOutputFactory,
                    _cfgNameForTextElement,
                    _nameProcessor);
        }
    }
}
