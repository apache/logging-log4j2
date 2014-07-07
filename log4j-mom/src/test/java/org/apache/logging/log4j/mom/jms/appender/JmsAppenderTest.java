package org.apache.logging.log4j.mom.jms.appender;

import javax.jms.Message;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockejb.jms.MockQueue;
import org.mockejb.jms.QueueConnectionFactoryImpl;
import org.mockejb.jndi.MockContextFactory;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class JmsAppenderTest {

    private static final String CONNECTION_FACTORY_NAME = "jms/activemq";
    private static final String DESTINATION_NAME = "jms/destination";
    private static final String LOG_MESSAGE = "Hello, world!";

    private static Context context;

    private JmsAppender appender;
    private static MockQueue queue;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MockContextFactory.setAsInitial();
        context = new InitialContext();
        context.rebind(CONNECTION_FACTORY_NAME, new QueueConnectionFactoryImpl());
        queue = new MockQueue(DESTINATION_NAME);
        context.rebind(DESTINATION_NAME, queue);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        Closer.closeSilently(context);
    }

    @Rule
    public InitialLoggerContext ctx = new InitialLoggerContext("JmsAppenderTest.xml");

    @Before
    public void setUp() throws Exception {
        appender = (JmsAppender) ctx.getAppender("JmsQueueAppender");
        assertEquals(0, queue.size());
    }

    @Test
    public void testAppendToQueue() throws Exception {
        final String loggerName = this.getClass().getName();
        final long now = System.currentTimeMillis();
        final LogEvent event = createLogEvent(loggerName, now);
        appender.append(event);
        assertEquals(1, queue.size());
        final Message message = queue.getMessageAt(0);
        assertNotNull(message);
        assertThat(message, instanceOf(TextMessage.class));
        final TextMessage textMessage = (TextMessage) message;
        assertEquals(LOG_MESSAGE, textMessage.getText());
    }

    private static Log4jLogEvent createLogEvent(String loggerName, long now) {
        return Log4jLogEvent.createEvent(loggerName, null, loggerName, Level.INFO,
            new SimpleMessage(LOG_MESSAGE), null, null, null, null, Thread.currentThread().getName(), null, now);
    }

}