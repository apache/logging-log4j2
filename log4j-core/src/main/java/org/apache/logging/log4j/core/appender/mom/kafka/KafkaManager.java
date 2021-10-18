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

package org.apache.logging.log4j.core.appender.mom.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.Log4jThread;

public class KafkaManager extends AbstractManager {

	public static final String DEFAULT_TIMEOUT_MILLIS = "30000";

	/**
	 * package-private access for testing.
	 */
	static KafkaProducerFactory producerFactory = new DefaultKafkaProducerFactory();

	private final Properties config = new Properties();
	private Producer<byte[], byte[]> producer;
	private final int timeoutMillis;

	private final String topic;
	private final String key;
	private final boolean syncSend;
	private static final KafkaManagerFactory factory = new KafkaManagerFactory();

	/*
	 * The Constructor should have been declared private as all Managers are create
	 * by the internal factory;
	 */
	public KafkaManager(final LoggerContext loggerContext, final String name, final String topic,
			final boolean syncSend, final Property[] properties, final String key) {
		super(loggerContext, name);
		this.topic = Objects.requireNonNull(topic, "topic");
		this.syncSend = syncSend;

		config.setProperty("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		config.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		config.setProperty("batch.size", "0");

		for (final Property property : properties) {
			config.setProperty(property.getName(), property.getValue());
		}

		this.key = key;

		this.timeoutMillis = Integer.parseInt(config.getProperty("timeout.ms", DEFAULT_TIMEOUT_MILLIS));
	}

	@Override
	public boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
		if (timeout > 0) {
			closeProducer(timeout, timeUnit);
		} else {
			closeProducer(timeoutMillis, TimeUnit.MILLISECONDS);
		}
		return true;
	}

	private void closeProducer(final long timeout, final TimeUnit timeUnit) {
		if (producer != null) {
			// This thread is a workaround for this Kafka issue:
			// https://issues.apache.org/jira/browse/KAFKA-1660
			final Thread closeThread = new Log4jThread(() -> {
            	if (producer != null) {
            		producer.close();
            	}
            }, "KafkaManager-CloseThread");
			closeThread.setDaemon(true); // avoid blocking JVM shutdown
			closeThread.start();
			try {
				closeThread.join(timeUnit.toMillis(timeout));
			} catch (final InterruptedException ignore) {
				Thread.currentThread().interrupt();
				// ignore
			}
		}
	}

	public void send(final byte[] msg) throws ExecutionException, InterruptedException, TimeoutException {
		if (producer != null) {
			byte[] newKey = null;

			if (key != null && key.contains("${")) {
				newKey = getLoggerContext().getConfiguration().getStrSubstitutor().replace(key)
						.getBytes(StandardCharsets.UTF_8);
			} else if (key != null) {
				newKey = key.getBytes(StandardCharsets.UTF_8);
			}

			final ProducerRecord<byte[], byte[]> newRecord = new ProducerRecord<>(topic, newKey, msg);
			if (syncSend) {
				final Future<RecordMetadata> response = producer.send(newRecord);
				response.get(timeoutMillis, TimeUnit.MILLISECONDS);
			} else {
				producer.send(newRecord, (metadata, e) -> {
                	if (e != null) {
                		LOGGER.error("Unable to write to Kafka in appender [" + getName() + "]", e);
                	}
                });
			}
		}
	}

	public void startup() {
		if (producer == null) {
			producer = producerFactory.newKafkaProducer(config);
		}
	}

	public String getTopic() {
		return topic;
	}

	public static KafkaManager getManager(final LoggerContext loggerContext, final String name, final String topic,
			final boolean syncSend, final Property[] properties, final String key) {
		StringBuilder sb = new StringBuilder(name);
		sb.append(" ").append(topic).append(" ").append(syncSend + "");
		for (Property prop : properties) {
			sb.append(" ").append(prop.getName()).append("=").append(prop.getValue());
		}
		return getManager(sb.toString(), factory, new FactoryData(loggerContext, topic, syncSend, properties, key));
	}

	private static class FactoryData {
		private final LoggerContext loggerContext;
		private final String topic;
		private final boolean syncSend;
		private final Property[] properties;
		private final String key;

		public FactoryData(final LoggerContext loggerContext, final String topic, final boolean syncSend,
				final Property[] properties, final String key) {
			this.loggerContext = loggerContext;
			this.topic = topic;
			this.syncSend = syncSend;
			this.properties = properties;
			this.key = key;
		}

	}

	private static class KafkaManagerFactory implements ManagerFactory<KafkaManager, FactoryData> {
		@Override
		public KafkaManager createManager(String name, FactoryData data) {
			return new KafkaManager(data.loggerContext, name, data.topic, data.syncSend, data.properties, data.key);
		}
	}

}
