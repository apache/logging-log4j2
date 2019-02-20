package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This test programmatically switches appenders. In cases where it is important that logged messages 
 * go to the appender that was configured at the time of writing the log event, the flushing the 
 * appender buffer to disk is not sufficient, as the log events may still be in the disruptor. The change
 * associated with this test has provided access to test if the disruptor has items in its backlog. This becomes 
 * useful in an application that is controlling its logging config and logging events to ensure that no new logging events 
 * are generated between removing one appender and adding another, and that there are no events remaining in the 
 * disruptor before the old appender is removed.
 *
 */
public class AsyncLoggerContextCheckDisruptorBacklogTest {
    private static AtomicBoolean failed = new AtomicBoolean(false);
    static {
        System.setProperty("log4j2.contextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
    }

    @Before
    public void setUp() {
        failed.set(false);
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setPackages("org.apache.logging.log4j.core.async");
        RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.ERROR);

        AppenderComponentBuilder appender1 = builder.newAppender("1", "TestAppender");
        AppenderComponentBuilder appender2 = builder.newAppender("2", "TestAppender");
        
        builder.add(appender1);
        builder.add(appender2);
        
        rootLogger.add(builder.newAppenderRef("1"));
        rootLogger.addAttribute("level", Level.ALL);
        builder.add(rootLogger);
        
        Configurator.initialize(builder.build());

    }

    @Test
    public void test() {
        final Logger logger = LogManager.getLogger();
        for(int i = 0; i < 10000; i++ ) {
            switchLogging(logger, "1", "2");
            switchLogging(logger, "2", "1");
        }
        Assert.assertFalse("Appenders got events meant for different appender", failed.get());
    }

    private void switchLogging(Logger logger, String from, String to) {
        LoggerContext loggerContext = LoggerContext.getContext(false);
        LoggerConfig loggerConfig = loggerContext.getConfiguration().getRootLogger();
        Appender toAppender = loggerContext.getConfiguration().getAppender(to);
       
        logger.info("{}", from);
        //The feature that was added that ensures this test passes by allowing disruptor to drain before 
        //programatically changing the appenders.
        while(((AsyncLoggerContext)loggerContext).hasBacklog()) {
            Thread.yield();
        }
        loggerConfig.removeAppender(from);
        loggerConfig.addAppender(toAppender, null, null);
        logger.info("{}", to);
    }

    @Plugin(name = "TestAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
    public static class TestAppender extends AbstractAppender {

        protected TestAppender(String name) {
            super(name, null, null, true, null);
        }

       @PluginFactory
       public static TestAppender createAppender(@PluginAttribute("name") String name,@PluginElement("Filter") Filter filter) {
           return new TestAppender(name);
       }

        @Override
        public void append(LogEvent event) {
            if(!failed.get()) {
                failed.set(!event.getMessage().getParameters()[0].equals(getName()));
            }
        }
    }
}
