package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public interface Configuration {

    LoggerConfig getLoggerConfig(String name);

    Map<String, Appender> getAppenders();

    Iterator<Filter> getFilters();

    void addFilter(Filter filter);

    void removeFilter(Filter filter);

    boolean hasFilters();

    Map<String, LoggerConfig> getLoggers();

    void addLoggerAppender(String name, Appender appender);    

    void start();

    void stop();

}
