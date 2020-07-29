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
package org.apache.log4j;

import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Implement this interface for your own strategies for outputting log
 * statements.
 */
public interface Appender {

    /**
     * Add a filter to the end of the filter list.
     * @param newFilter The filter to add.
     *
     * @since 0.9.0
     */
    void addFilter(Filter newFilter);

    /**
     * Returns the head Filter. The Filters are organized in a linked list
     * and so all Filters on this Appender are available through the result.
     *
     * @return the head Filter or null, if no Filters are present
     * @since 1.1
     */
    Filter getFilter();

    /**
     * Clear the list of filters by removing all the filters in it.
     *
     * @since 0.9.0
     */
    void clearFilters();

    /**
     * Release any resources allocated within the appender such as file
     * handles, network connections, etc.
     * <p>
     * It is a programming error to append to a closed appender.
     * </p>
     *
     * @since 0.8.4
     */
    void close();

    /**
     * Log in <code>Appender</code> specific way. When appropriate,
     * Loggers will call the <code>doAppend</code> method of appender
     * implementations in order to log.
     * @param event The LoggingEvent.
     */
    void doAppend(LoggingEvent event);


    /**
     * Get the name of this appender.
     *
     * @return name, may be null.
     */
    String getName();


    /**
     * Set the {@link ErrorHandler} for this appender.
     * @param errorHandler The error handler.
     *
     * @since 0.9.0
     */
    void setErrorHandler(ErrorHandler errorHandler);

    /**
     * Returns the {@link ErrorHandler} for this appender.
     * @return The error handler.
     *
     * @since 1.1
     */
    ErrorHandler getErrorHandler();

    /**
     * Set the {@link Layout} for this appender.
     * @param layout The Layout.
     *
     * @since 0.8.1
     */
    void setLayout(Layout layout);

    /**
     * Returns this appenders layout.
     * @return the Layout.
     *
     * @since 1.1
     */
    Layout getLayout();


    /**
     * Set the name of this appender. The name is used by other
     * components to identify this appender.
     * @param name The appender name.
     *
     * @since 0.8.1
     */
    void setName(String name);

    /**
     * Configurators call this method to determine if the appender
     * requires a layout. If this method returns {@code true},
     * meaning that layout is required, then the configurator will
     * configure an layout using the configuration information at its
     * disposal.  If this method returns {@code false}, meaning that
     * a layout is not required, then layout configuration will be
     * skipped even if there is available layout configuration
     * information at the disposal of the configurator..
     * <p>
     * In the rather exceptional case, where the appender
     * implementation admits a layout but can also work without it, then
     * the appender should return {@code true}.
     * </p>
     * @return true if a Layout is required.
     *
     * @since 0.8.4
     */
    boolean requiresLayout();
}

