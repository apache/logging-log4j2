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

import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.util.Strings;

/**
 *
 */
public abstract class Layout {

    /** Note that the line.separator property can be looked up even by applets. */
    public static final int LINE_SEP_LEN = Strings.LINE_SEPARATOR.length();

    /**
     * Implement this method to create your own layout format.
     * @param event The LoggingEvent.
     * @return The formatted LoggingEvent.
     */
    public abstract String format(LoggingEvent event);

    /**
     * Returns the content type output by this layout. The base class
     * returns "text/plain".
     * @return the type of content rendered by the Layout.
     */
    public String getContentType() {
        return "text/plain";
    }

    /**
     * Returns the header for the layout format. The base class returns
     * <code>null</code>.
     * @return The header.
     */
    public String getHeader() {
        return null;
    }

    /**
     * Returns the footer for the layout format. The base class returns
     * <code>null</code>.
     * @return The footer.
     */
    public String getFooter() {
        return null;
    }


    /**
     * If the layout handles the throwable object contained within
     * {@link LoggingEvent}, then the layout should return
     * {@code false}. Otherwise, if the layout ignores throwable
     * object, then the layout should return {@code true}.
     * If ignoresThrowable is true, the appender is responsible for
     * rendering the throwable.
     * <p>
     * The <a href="/log4j/1.2/apidocs/org/apache/log4j/SimpleLayout.html">SimpleLayout</a>,
     * <a href="/log4j/1.2/apidocs/org/apache/log4j/TTCCLayout.html">TTCCLayout</a>,
     * <a href="/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html">PatternLayout</a>
     * all return {@code true}. The
     * <a href="/log4j/1.2/apidocs/org/apache/log4j/xml/XMLLayout.html">XMLLayout</a>
     * returns {@code false}.
     * </p>
     *
     * @return true if the Layout ignores Throwables.
     *
     * @since 0.8.4
     */
    public abstract boolean ignoresThrowable();
}

