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
package org.apache.logging.log4j.jackson.json.template.layout;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.jackson.json.template.layout.util.ByteBufferOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.function.Supplier;

enum JsonTemplateLayoutSerializationContexts {;

    private static final SerializedString EMPTY_SERIALIZED_STRING = new SerializedString("");

    private static final PrettyPrinter PRETTY_PRINTER = new DefaultPrettyPrinter("");

    static Supplier<JsonTemplateLayoutSerializationContext> createSupplier(
            final ObjectMapper objectMapper,
            final int maxByteCount,
            final boolean prettyPrintEnabled,
            final boolean emptyPropertyExclusionEnabled,
            final int maxStringLength) {
        final JsonFactory jsonFactory = new JsonFactory(objectMapper);
        return () -> {
            final ByteBufferOutputStream outputStream = new ByteBufferOutputStream(maxByteCount);
            final JsonGenerator jsonGenerator = createJsonGenerator(
                    jsonFactory,
                    outputStream,
                    prettyPrintEnabled,
                    emptyPropertyExclusionEnabled,
                    maxStringLength);
            return new JsonTemplateLayoutSerializationContext() {

                @Override
                public ByteBufferOutputStream getOutputStream() {
                    return outputStream;
                }

                @Override
                public JsonGenerator getJsonGenerator() {
                    return jsonGenerator;
                }

                @Override
                public void close() throws Exception {
                    jsonGenerator.close();
                }

                @Override
                public void reset() {
                    outputStream.getByteBuffer().clear();
                }

            };
        };
    }

    private static JsonGenerator createJsonGenerator(
            final JsonFactory jsonFactory,
            final OutputStream outputStream,
            final boolean prettyPrintEnabled,
            final boolean emptyPropertyExclusionEnabled,
            final int maxStringLength) {
        try {
            JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream);
            jsonGenerator.setRootValueSeparator(EMPTY_SERIALIZED_STRING);
            if (prettyPrintEnabled) {
                jsonGenerator.setPrettyPrinter(PRETTY_PRINTER);
            }
            if (emptyPropertyExclusionEnabled) {
                jsonGenerator = new FilteringGeneratorDelegate(
                        jsonGenerator, NullExcludingTokenFilter.INSTANCE, true, true);
            }
            if (maxStringLength > 0) {
                jsonGenerator = new StringTruncatingGeneratorDelegate(
                        jsonGenerator, maxStringLength);
            }
            return jsonGenerator;
        } catch (final IOException error) {
            throw new RuntimeException("failed creating JsonGenerator", error);
        }
    }

    private static class NullExcludingTokenFilter extends TokenFilter {

        private static final NullExcludingTokenFilter INSTANCE =
                new NullExcludingTokenFilter();

        @Override
        public boolean includeNull() {
            return false;
        }

    }

    private static class StringTruncatingGeneratorDelegate
            extends JsonGeneratorDelegate {

        private final int maxStringLength;

        private StringTruncatingGeneratorDelegate(
                final JsonGenerator jsonGenerator,
                final int maxStringLength) {
            super(jsonGenerator);
            this.maxStringLength = maxStringLength;
        }

        @Override
        public void writeString(final String text) throws IOException {
            if (text == null) {
                writeNull();
            } else if (maxStringLength <= 0 || maxStringLength >= text.length()) {
                super.writeString(text);
            } else {
                final StringReader textReader = new StringReader(text);
                super.writeString(textReader, maxStringLength);
            }
        }

        @Override
        public void writeFieldName(final String name) throws IOException {
            if (maxStringLength <= 0 || maxStringLength >= name.length()) {
                super.writeFieldName(name);
            } else {
                final String truncatedName = name.substring(0, maxStringLength);
                super.writeFieldName(truncatedName);
            }
        }

    }

}
