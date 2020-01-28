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
package org.apache.logging.log4j.jackson.json.template.layout.util;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.util.Constants;

import java.io.IOException;
import java.math.BigDecimal;

public enum JsonGenerators {;

    private static final class DoubleWriterContext {

        private static final int MAX_BUFFER_LENGTH =
                String.format("%d.%d", Long.MAX_VALUE, Integer.MAX_VALUE).length()
                        + 1;

        private final StringBuilder builder = new StringBuilder();

        private char[] buffer = new char[MAX_BUFFER_LENGTH];

    }

    private static final ThreadLocal<DoubleWriterContext> DOUBLE_WRITER_CONTEXT_REF =
            Constants.ENABLE_THREADLOCALS
                    ? ThreadLocal.withInitial(DoubleWriterContext::new)
                    : null;

    public static void writeDouble(
            final JsonGenerator jsonGenerator,
            final long integralPart,
            final int fractionalPart)
            throws IOException {
        if (fractionalPart < 0) {
            throw new IllegalArgumentException("negative fraction");
        } else if (fractionalPart == 0) {
            jsonGenerator.writeNumber(integralPart);
        } else if (Constants.ENABLE_THREADLOCALS) {
            final DoubleWriterContext context = DOUBLE_WRITER_CONTEXT_REF.get();
            context.builder.setLength(0);
            context.builder.append(integralPart);
            context.builder.append('.');
            context.builder.append(fractionalPart);
            final int length = context.builder.length();
            context.builder.getChars(0, length, context.buffer, 0);
            // jackson-core version >=2.10.2 is required for the following line
            // to avoid FasterXML/jackson-core#588 bug.
            jsonGenerator.writeRawValue(context.buffer, 0, length);
        } else {
            final String formattedNumber = "" + integralPart + '.' + fractionalPart;
            jsonGenerator.writeNumber(formattedNumber);
        }
    }

    /**
     * Writes given object, preferably using GC-free writers.
     */
    public static void writeObject(
            final JsonGenerator jsonGenerator,
            final Object object)
            throws IOException {

        if (object == null) {
            jsonGenerator.writeNull();
            return;
        }

        if (object instanceof String) {
            jsonGenerator.writeString((String) object);
            return;
        }

        if (object instanceof Short) {
            jsonGenerator.writeNumber((Short) object);
            return;
        }

        if (object instanceof Integer) {
            jsonGenerator.writeNumber((Integer) object);
            return;
        }

        if (object instanceof Long) {
            jsonGenerator.writeNumber((Long) object);
            return;
        }

        if (object instanceof BigDecimal) {
            jsonGenerator.writeNumber((BigDecimal) object);
            return;
        }

        if (object instanceof Float) {
            jsonGenerator.writeNumber((Float) object);          // Not GC-free!
            return;
        }

        if (object instanceof Double) {
            jsonGenerator.writeNumber((Double) object);         // Not GC-free!
            return;
        }

        if (object instanceof byte[]) {
            jsonGenerator.writeBinary((byte[]) object);
            return;
        }

        jsonGenerator.writeObject(object);

    }

}
