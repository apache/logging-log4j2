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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.core.LogEvent;

import java.util.List;

/**
 * Primary appenders used with {@link FailoverAppender}s that implement this interface will be aware of events in the
 * parent {@link FailoverAppender}.
 */
public interface FailoverAware {
    
    /**
     * Invoked when failing over from a primary appender to a secondary appender.
     * @param event the event that triggered the failover
     * @param exception the exception that caused the failover
     * @return a {@link List} of {@link LogEvent}s to be passed to the secondary appenders
     */
    List<LogEvent> onFailover(LogEvent event, Exception exception);

    /**
     * Invoked when the parent {@link FailoverAppender} is preparing to stop.  Nothing is actually stopped at this point.
     */
    void beforeFailoverAppenderStop();

    /**
     * Invoked if an error occurred when {@link #beforeFailoverAppenderStop()} was invoked.
     * @param exception the exception thrown from {@link #beforeFailoverAppenderStop()}
     * @return a {@link List} of {@link LogEvent}s to be passed to the secondary appenders
     */
    List<LogEvent> beforeFailoverAppenderStopException(Exception exception);
    
}
