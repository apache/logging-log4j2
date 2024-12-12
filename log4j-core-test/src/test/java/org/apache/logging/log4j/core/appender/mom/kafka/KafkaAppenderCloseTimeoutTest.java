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
package org.apache.logging.log4j.core.appender.mom.kafka;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Tag("Appenders.Kafka")
@LoggerContextSource("KafkaAppenderCloseTimeoutTest.xml")
public class KafkaAppenderCloseTimeoutTest {

    private static final Serializer<byte[]> SERIALIZER = new ByteArraySerializer();

    private static final MockProducer<byte[], byte[]> kafka =
            new MockProducer<byte[], byte[]>(true, SERIALIZER, SERIALIZER) {
                @Override
                public void close() {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignore) {
                        // NOP
                    }
                }

                // @Override in version 3.3.1
                public void close(final Duration timeout) {
                    try {
                        Thread.sleep(timeout.toMillis());
                    } catch (InterruptedException ignore) {
                        // NOP
                    }
                }

                // @Override in version 1.1.1
                public void close(final long timeout, final TimeUnit timeUnit) {
                    try {
                        Thread.sleep(timeUnit.toMillis(timeout));
                    } catch (InterruptedException ignore) {
                        // NOP
                    }
                }
            };

    @BeforeAll
    public static void setUpAll() {
        KafkaManager.producerFactory = config -> kafka;
    }

    @BeforeEach
    public void setUp() {
        kafka.clear();
    }

    @Test
    @Timeout(2000)
    public void testClose(@Named("KafkaAppender") final Appender appender) {
        assertNotNull(appender, "Appender named KafkaAppender was null.");
        appender.stop();
    }
}
