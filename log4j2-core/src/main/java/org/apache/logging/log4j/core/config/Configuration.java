package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public interface Configuration {

    String getName();

    LoggerConfig getLoggerConfig(String name);

    Map<String, Appender> getAppenders();

    Iterator<Filter> getFilters();

    void addFilter(Filter filter);

    void removeFilter(Filter filter);

    boolean hasFilters();

    Map<String, LoggerConfig> getLoggers();

    void addLoggerAppender(Logger logger, Appender appender);

    void addLoggerFilter(Logger logger, Filter filter);

    void setLoggerAdditive(Logger logger, boolean additive);

    void start();

    void stop();

}
