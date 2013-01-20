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
 *
 * @param <T> The {@link Layout}'s {@link Serializable} type.
 */
public interface Appender<T extends Serializable> extends LifeCycle {

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
    Layout<T> getLayout();

    /**
     * If set to true any exceptions thrown by the Appender will be logged but not thrown.
     * @return true if Exceptions should be suppressed, false otherwise.
     */
    boolean isExceptionSuppressed();

    ErrorHandler getHandler();

    void setHandler(ErrorHandler handler);

}
