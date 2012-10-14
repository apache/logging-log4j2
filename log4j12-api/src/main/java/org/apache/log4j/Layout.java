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

/**
 *
 */
public abstract class Layout {

    // Note that the line.separator property can be looked up even by
    // applets.
    public final static String LINE_SEP = System.getProperty("line.separator");
    public final static int LINE_SEP_LEN = LINE_SEP.length();


    /**
     * Implement this method to create your own layout format.
     */
    public abstract String format(LoggingEvent event);

    /**
     * Returns the content type output by this layout. The base class
     * returns "text/plain".
     */
    public String getContentType() {
        return "text/plain";
    }

    /**
     * Returns the header for the layout format. The base class returns
     * <code>null</code>.
     */
    public String getHeader() {
        return null;
    }

    /**
     * Returns the footer for the layout format. The base class returns
     * <code>null</code>.
     */
    public String getFooter() {
        return null;
    }


    /**
     * If the layout handles the throwable object contained within
     * {@link LoggingEvent}, then the layout should return
     * <code>false</code>. Otherwise, if the layout ignores throwable
     * object, then the layout should return <code>true</code>.
     * If ignoresThrowable is true, the appender is responsible for
     * rendering the throwable.
     * <p/>
     * <p>The {@link SimpleLayout}, {@link TTCCLayout}, {@link
     * PatternLayout} all return <code>true</code>. The {@link
     * org.apache.log4j.xml.XMLLayout} returns <code>false</code>.
     *
     * @since 0.8.4
     */
    public abstract boolean ignoresThrowable();
}

