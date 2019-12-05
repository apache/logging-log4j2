package org.apache.logging.log4j.kafka.appender;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.KafkaException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Properties;

@Category(Appenders.Kafka.class)
public class KafkaAppenderConnectionErrorTest {
    @BeforeClass
    public static void setUpClass() {
        KafkaManager.producerFactory = new KafkaProducerFactory() {
            @Override
            public Producer<byte[], byte[]> newKafkaProducer(Properties config) {
                throw new KafkaException();
            }
        };
    }

    @Rule
    public LoggerContextRule ctx = new LoggerContextRule("KafkaConnectionErrorTest.xml");

    private static final String LOG_MESSAGE = "Hello, world!";

    private static Log4jLogEvent createLogEvent() {
        return Log4jLogEvent.newBuilder()
                            .setLoggerName(KafkaAppenderTest.class.getName())
                            .setLoggerFqcn(KafkaAppenderTest.class.getName())
                            .setLevel(Level.INFO)
                            .setMessage(new SimpleMessage(LOG_MESSAGE))
                            .build();
    }

    @Test
    public void testAppendWithKafkaConnectionError() {
        final Appender appender = ctx.getRequiredAppender("KafkaConnectionErrorIgnoredTest");
        appender.append(createLogEvent());
    }
}
