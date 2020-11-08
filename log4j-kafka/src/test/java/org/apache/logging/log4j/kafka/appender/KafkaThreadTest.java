package org.apache.logging.log4j.kafka.appender;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class KafkaThreadTest {

    @Rule
    public LoggerContextRule ctx = new LoggerContextRule("KafkaAppenderTest.xml");

    @Test
    public void testKafkaThreadLeak(){
        LoggerContext context = ctx.getLoggerContext();
        context.getContext(false).reconfigure();
        context.getContext(false).reconfigure();
        Map<Thread, StackTraceElement[]> maps = Thread.getAllStackTraces();
        int kafkaThreadCount = 0;
        if (maps != null && maps.size() >0) {
            for (Thread thread : maps.keySet()) {
                if (thread.getName().startsWith("kafka-producer")){
                    kafkaThreadCount++;
                }
            }
        }
        assertTrue(kafkaThreadCount <= 5);
    }

}
