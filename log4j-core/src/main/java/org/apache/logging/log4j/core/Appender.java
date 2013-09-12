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
package org.apache.logging.log4j.core;

import java.io.Serializable;

/**
 * Appends log events.
 */
public interface Appender extends LifeCycle {

    /**
     * Log in <code>Appender</code> specific way. When appropriate,
     * Loggers will call the <code>doAppend</code> method of appender
     * implementations in order to log.
     *
     * @param event The LogEvent.
     */
    void append(LogEvent event);


    /**
     * Get the name of this appender.
     *
     * @return name, may be null.
     */
    String getName();

    /**
     * Returns this appender's layout.
     *
     * @return the Layout for the Appender or null if none is configured.
     */
    Layout<? extends Serializable> getLayout();

    /**
     * Some appenders need to propagate exceptions back to the application. When {@code ignoreExceptions} is
     * {@code false} the AppenderControl will allow the exception to percolate.
     *
     * @return {@code true} if exceptions will be logged but now thrown, {@code false} otherwise.
     */
    boolean ignoreExceptions();

    ErrorHandler getHandler();

    void setHandler(ErrorHandler handler);
}
