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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class JsonLayoutJsonTemplateGeneratorStateResetTest {

    private static final Configuration CONFIG = new DefaultConfiguration();

    private static final JsonTemplateLayout LAYOUT = JsonTemplateLayout
            .newBuilder()
            .setConfiguration(CONFIG)
            .setEventTemplate("{\"message\": \"${json:message}\"}")
            .setStackTraceEnabled(true)
            .setLocationInfoEnabled(true)
            .setBlankPropertyExclusionEnabled(true)
            .build();

    private static final JsonFactory JSON_FACTORY = ObjectMapperFixture.getObjectMapper().getFactory();

    private static final int MAX_BYTE_COUNT = JsonTemplateLayout.newBuilder().getMaxByteCount();

    private static final LogEvent HUGE_LOG_EVENT = createLogEventExceedingMaxByteCount();

    private static final LogEvent LITE_LOG_EVENT = LogEventFixture.createLiteLogEvents(1).get(0);

    private static LogEvent createLogEventExceedingMaxByteCount() {
        final byte[] messageBytes = generateRandomBytes(MAX_BYTE_COUNT);
        final String messageText = new String(messageBytes);
        final SimpleMessage message = new SimpleMessage(messageText);
        return Log4jLogEvent
                .newBuilder()
                .setMessage(message)
                .build();
    }

    private static byte[] generateRandomBytes(final int length) {
        final byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = (byte) (Math.random() * 0xFF);
        }
        return buffer;
    }

    @Test
    public void test_toByteArray() throws Exception {
        test_serializer_recover_after_buffer_overflow(LAYOUT::toByteArray);
    }

    @Test
    public void test_toSerializable() throws Exception {
        test_serializer_recover_after_buffer_overflow((final LogEvent logEvent) -> {
            final String serializableLogEvent = LAYOUT.toSerializable(logEvent);
            return serializableLogEvent.getBytes(StandardCharsets.UTF_8);
        });
    }

    @Test
    public void test_encode() throws Exception {
        final FixedByteBufferDestination destination = new FixedByteBufferDestination(MAX_BYTE_COUNT);
        test_serializer_recover_after_buffer_overflow((final LogEvent logEvent) -> {
            LAYOUT.encode(logEvent, destination);
            return copyWrittenBytes(destination.getByteBuffer());
        });
    }

    private byte[] copyWrittenBytes(final ByteBuffer byteBuffer) {
        final Buffer flip = byteBuffer.flip();
        final byte[] writtenBytes = new byte[flip.remaining()];
        byteBuffer.get(writtenBytes);
        return writtenBytes;
    }

    private void test_serializer_recover_after_buffer_overflow(
            final ThrowingFunction<LogEvent, byte[]> serializer)
            throws Exception {
        Assertions
                .assertThatThrownBy(() -> serializer.apply(HUGE_LOG_EVENT))
                .hasCauseInstanceOf(BufferOverflowException.class);
        test_JsonGenerator_state_reset();
        test_jsonBytes(serializer.apply(LITE_LOG_EVENT));
    }

    private void test_jsonBytes(final byte[] jsonBytes) {
        final String json = new String(jsonBytes, StandardCharsets.UTF_8);
        Assertions
                .assertThatCode(() -> {
                    final JsonParser parser = JSON_FACTORY.createParser(json);
                    // noinspection StatementWithEmptyBody (consume each token)
                    while (parser.nextToken() != null) ;
                })
                .as("should be a valid JSON: %s", json)
                .doesNotThrowAnyException();
    }

    private void test_JsonGenerator_state_reset() {
        final JsonTemplateLayoutSerializationContext serializationContext =
                LAYOUT.getSerializationContext();
        final ByteBuffer byteBuffer = serializationContext.getOutputStream().getByteBuffer();
        final JsonGenerator jsonGenerator = serializationContext.getJsonGenerator();
        final JsonStreamContext outputContext = jsonGenerator.getOutputContext();
        Assertions.assertThat(outputContext.inRoot()).isTrue();
        Assertions.assertThat(outputContext.inObject()).isFalse();
        Assertions.assertThat(outputContext.inArray()).isFalse();
        Assertions.assertThat(byteBuffer.position()).isEqualTo(0);
    }

    private interface ThrowingFunction<I, O> {

        O apply(I input) throws Exception;

    }

}
