/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.logging.log4j.pulsar.appender;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Property;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageBuilder;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.ProducerConfiguration;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

public class PulsarManager extends AbstractManager {

    static PulsarFactory PULSAR_CLIENT_FACTORY = new PulsarFactory() {
        @Override
        public PulsarClient createClient(String serviceUrl) throws Exception {
            return PulsarClient.create(serviceUrl);
        }
    };

    static MessageFactory MESSAGE_FACTORY = new MessageFactory() {
        @Override
        public Message create(String key, byte[] value) {
            MessageBuilder messageBuilder = MessageBuilder.create()
                .setContent(value);
            if (null != key) {
                messageBuilder = messageBuilder.setKey(key);
            }
            return messageBuilder.build();
        }
    };

    private PulsarClient client;
    private Producer producer;

    private final String serviceUrl;
    private final String topic;
    private final String key;
    private final boolean syncSend;

    public PulsarManager(final LoggerContext loggerContext,
                         final String name,
                         final String serviceUrl,
                         final String topic,
                         final boolean syncSend,
                         final Property[] properties,
                         final String key) {
        super(loggerContext, name);
        this.serviceUrl = Objects.requireNonNull(serviceUrl, "serviceUrl");
        this.topic = Objects.requireNonNull(topic, "topic");
        this.syncSend = syncSend;
        this.key = key;
    }

    @Override
    public boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        if (producer != null) {
            try {
                producer.close();
            } catch (PulsarClientException e) {
                // exceptions on closing
            }
        }
        return true;
    }

    public void send(final byte[] msg)  {
        if (producer != null) {
            String newKey = null;

            if(key != null && key.contains("${")) {
                newKey = getLoggerContext().getConfiguration().getStrSubstitutor().replace(key);
            } else if (key != null) {
                newKey = key;
            }

            Message message = MESSAGE_FACTORY.create(newKey, msg);
            if (syncSend) {
                try {
                    producer.send(message);
                } catch (PulsarClientException e) {
                    LOGGER.error("Unable to write to Pulsar in appender [" + getName() + "]", e);
                }
            } else {
                producer.sendAsync(message)
                    .exceptionally(cause -> {
                        LOGGER.error("Unable to write to Pulsar in appender [" + getName() + "]", cause);
                        return null;
                    });
            }
        }
    }

    public void startup() throws Exception {
        try {
            client = PULSAR_CLIENT_FACTORY.createClient(serviceUrl);

            ProducerConfiguration conf = new ProducerConfiguration();
            conf.setProducerName("pulsar-log4j2-appender-" + topic);
            conf.setBatchingEnabled(true);
            conf.setBatchingMaxPublishDelay(1, TimeUnit.MILLISECONDS);
            conf.setBlockIfQueueFull(false);

            producer = client.createProducer(topic, conf);
        } catch (Exception t) {
            LOGGER.error("Failed to start pulsar manager {}", t);
            throw t;
        }
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public String getTopic() {
        return topic;
    }

}
