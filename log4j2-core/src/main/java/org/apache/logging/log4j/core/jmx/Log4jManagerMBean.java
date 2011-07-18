package org.apache.logging.log4j.core.jmx;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.internal.StatusData;

import java.util.List;

/**
 *
 */
public interface Log4jManagerMBean {

    public List<LoggerContext> getLoggerContexts();

    public List<StatusData> getStatusData();
}
