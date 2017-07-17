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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;


public class FailoverAwareTest {
    
    private Configuration configuration = null;
    private FailoverAppender failoverAppender = null;
    private FailoverAwareAppender primaryAppender = null;
    private Appender secondaryAppender = null;
    private ErrorHandler primaryErrorHandler = null;

    @Before
    public void setup() {
        
        primaryAppender = mock(FailoverAwareAppender.class);
        secondaryAppender = mock(Appender.class);
        primaryErrorHandler = mock(ErrorHandler.class);

        when(primaryAppender.isStarted()).thenReturn(true);
        when(primaryAppender.getHandler()).thenReturn(primaryErrorHandler);
        when(secondaryAppender.isStarted()).thenReturn(true);
        

        Map<String, Appender> appenders = new HashMap<>();
        appenders.put("primary", primaryAppender);
        appenders.put("secondary", secondaryAppender);

        configuration = mock(Configuration.class);
        when(configuration.getAppenders()).thenReturn(Collections.unmodifiableMap(appenders));

        failoverAppender = FailoverAppender.createAppender(
                "failover",
                "primary",
                new String[]{"secondary"},
                "0",
                configuration,
                null,
                "false");
        failoverAppender.start();

    }

    @Test
    public void testOnBeforeStop() throws Exception {
        
        failoverAppender.beforeStopConfiguration();

        verify(primaryAppender).beforeFailoverAppenderStop();
        verify(primaryAppender, never()).beforeFailoverAppenderStopException(any(Exception.class));
        verify(secondaryAppender, never()).append(any(LogEvent.class));
    }

    @Test
    public void testOnBeforeStopExceptionNoResults() throws Exception {
        
        RuntimeException exception = new RuntimeException("test");
        List<LogEvent> events = Collections.emptyList();
        doThrow(exception).when(primaryAppender).beforeFailoverAppenderStop();
        when(primaryAppender.beforeFailoverAppenderStopException(exception)).thenReturn(events);

        failoverAppender.beforeStopConfiguration();

        verify(primaryAppender).beforeFailoverAppenderStop();
        verify(primaryAppender).beforeFailoverAppenderStopException(exception);
        verify(secondaryAppender, never()).append(any(LogEvent.class));
    }

    @Test
    public void testOnBeforeStopExceptionWithResults() throws Exception {
        
        LogEvent event1 = mock(LogEvent.class);
        LogEvent event2 = mock(LogEvent.class);

        RuntimeException exception = new RuntimeException("test");
        List<LogEvent> events = Arrays.asList(event1, event2);
        doThrow(exception).when(primaryAppender).beforeFailoverAppenderStop();
        when(primaryAppender.beforeFailoverAppenderStopException(exception)).thenReturn(events);

        failoverAppender.beforeStopConfiguration();

        verify(primaryAppender).beforeFailoverAppenderStop();
        verify(primaryAppender).beforeFailoverAppenderStopException(exception);
        verify(secondaryAppender).append(event1);
        verify(secondaryAppender).append(event2);
    }

    @Test
    public void testFailover() throws Exception {
        
        LogEvent event1 = mock(LogEvent.class);
        LogEvent event2 = mock(LogEvent.class);        
        List<LogEvent> events = Arrays.asList(event1, event2);
        
        RuntimeException exception = new RuntimeException("test");        
        when(primaryAppender.onFailover(event1, exception)).thenReturn(events);
        
        doThrow(exception).when(primaryAppender).append(event1);
        
        failoverAppender.append(event1);
        
        verify(secondaryAppender).append(event1);
        verify(secondaryAppender).append(event2);
        
    }

    private interface FailoverAwareAppender extends Appender, FailoverAware {}
}
