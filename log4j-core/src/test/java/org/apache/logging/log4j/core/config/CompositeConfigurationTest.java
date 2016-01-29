package org.apache.logging.log4j.core.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class CompositeConfigurationTest
{

    @Test
    public void compositeConfigurationUsed()
    {
        final LoggerContextRule lcr =
            new LoggerContextRule( "classpath:log4j-comp-appender.xml;log4j-comp-appender.json" );
        Statement test = new Statement()
        {
            @Override
            public void evaluate()
                throws Throwable
            {
                assertTrue( lcr.getConfiguration() instanceof CompositeConfiguration );
            }
        };
        runTest( lcr, test );
    }

    @Test
    public void compositeProperties()
    {
        final LoggerContextRule lcr =
            new LoggerContextRule( "classpath:log4j-comp-properties.xml;log4j-comp-properties.json" );
        Statement test = new Statement()
        {
            @Override
            public void evaluate()
                throws Throwable
            {
                CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
                assertEquals( "json", config.getStrSubstitutor().replace( "${propertyShared}" ) );
                assertEquals( "xml", config.getStrSubstitutor().replace( "${propertyXml}" ) );
                assertEquals( "json", config.getStrSubstitutor().replace( "${propertyJson}" ) );
            }
        };
        runTest( lcr, test );
    }

    @Test
    public void compositeAppenders()
    {
        final LoggerContextRule lcr =
            new LoggerContextRule( "classpath:log4j-comp-appender.xml;log4j-comp-appender.json" );
        Statement test = new Statement()
        {
            @Override
            public void evaluate()
                throws Throwable
            {
                CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
                Map<String, Appender> appender = config.getAppenders();
                assertEquals( 3, appender.size() );
                assertTrue( appender.get( "STDOUT" ) instanceof ConsoleAppender );
                assertTrue( appender.get( "File" ) instanceof FileAppender );
                assertTrue( appender.get( "Override" ) instanceof RollingFileAppender );
            }
        };
        runTest( lcr, test );
    }

    @Test
    public void compositeLogger()
    {
        final LoggerContextRule lcr = new LoggerContextRule( "classpath:log4j-comp-logger.xml;log4j-comp-logger.json" );
        Statement test = new Statement()
        {
            @Override
            public void evaluate()
                throws Throwable
            {
                CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
                Map<String, Appender> appendersMap = config.getLogger( "cat1" ).getAppenders();
                assertEquals( 1, appendersMap.size() );
                assertTrue( appendersMap.get( "STDOUT" ) instanceof ConsoleAppender );

                appendersMap = config.getLogger( "cat2" ).getAppenders();
                assertEquals( 1, appendersMap.size() );
                assertTrue( appendersMap.get( "File" ) instanceof FileAppender );

                appendersMap = config.getLogger( "cat3" ).getAppenders();
                assertEquals( 1, appendersMap.size() );
                assertTrue( appendersMap.get( "File" ) instanceof FileAppender );

                appendersMap = config.getRootLogger().getAppenders();
                assertEquals( 2, appendersMap.size() );
                assertTrue( appendersMap.get( "File" ) instanceof FileAppender );
                assertTrue( appendersMap.get( "STDOUT" ) instanceof ConsoleAppender );
            }
        };
        runTest( lcr, test );
    }

    @Test
    public void overrideFilter()
    {
        final LoggerContextRule lcr = new LoggerContextRule( "classpath:log4j-comp-filter.xml;log4j-comp-filter.json" );
        Statement test = new Statement()
        {
            @Override
            public void evaluate()
                throws Throwable
            {
                CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
                assertTrue( config.getFilter() instanceof ThresholdFilter );
            }
        };
        runTest( lcr, test );
    }

    private void runTest( LoggerContextRule rule, Statement statement )
    {
        try
        {
            rule.apply( statement, Description.createTestDescription( getClass(),
                Thread.currentThread().getStackTrace()[1].getMethodName() ) ).evaluate();
        }
        catch ( Throwable throwable )
        {
            throw new RuntimeException( throwable );
        }
    }
}
