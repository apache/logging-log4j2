/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.helpers;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;

/**
 * Formats a {@link Date} by printing the number of milliseconds elapsed since construction of the format. This is the
 * fastest printing DateFormat in the package.
 *
 * @since 0.7.5
 */
public class RelativeTimeDateFormat extends DateFormat {

    private static final long serialVersionUID = 7055751607085611984L;

    protected final long startTime;

    public RelativeTimeDateFormat() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Appends to <code>sbuf</code> the number of milliseconds elapsed since the start of the application.
     *
     * @since 0.7.5
     */
    @Override
    public StringBuffer format(final Date date, final StringBuffer sbuf, final FieldPosition fieldPosition) {
        // System.err.println(":"+ date.getTime() + " - " + startTime);
        return sbuf.append((date.getTime() - startTime));
    }

    /**
     * Always returns null.
     */
    @Override
    public Date parse(final String s, final ParsePosition pos) {
        return null;
    }
}
