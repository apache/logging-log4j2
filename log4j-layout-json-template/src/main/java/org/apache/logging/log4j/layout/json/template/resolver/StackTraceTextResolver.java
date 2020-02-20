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
package org.apache.logging.log4j.layout.json.template.resolver;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.layout.json.template.util.BufferedPrintWriter;
import org.apache.logging.log4j.layout.json.template.util.Recycler;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

final class StackTraceTextResolver implements StackTraceResolver {

    private final Recycler<BufferedPrintWriter> writerRecycler;

    StackTraceTextResolver(final EventResolverContext context) {
        final Supplier<BufferedPrintWriter> writerSupplier =
                () -> BufferedPrintWriter.ofCapacity(context.getWriterCapacity());
        final Function<BufferedPrintWriter, BufferedPrintWriter> writerCleaner =
                writer -> {
                    writer.close();
                    return writer;
                };
        this.writerRecycler = context.getRecyclerFactory().create(writerSupplier, writerCleaner);
    }

    @Override
    public void resolve(
            final Throwable throwable,
            final JsonGenerator jsonGenerator) throws IOException {
        final BufferedPrintWriter writer = writerRecycler.acquire();
        try {
            throwable.printStackTrace(writer);
            jsonGenerator.writeString(writer.getBuffer(), 0, writer.getPosition());
        } finally {
            writerRecycler.release(writer);
        }
    }

}
