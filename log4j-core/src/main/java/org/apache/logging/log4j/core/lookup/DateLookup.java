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
package org.apache.logging.log4j.core.lookup;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Formats the current date or the date in the LogEvent. The "key" is used as the format String,
 * following the java.text.SimpleDateFormat date and time pattern strings.
 */
@Plugin(name = "date", category = StrLookup.CATEGORY)
public class DateLookup implements StrLookup {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Marker LOOKUP = MarkerManager.getMarker("LOOKUP");

    /**
     * Looks up the current date.
     * @param key the format to use. If null, the default DateFormat will be used.
     * @return The formatted current date, never null.
     */
    @Override
    public String lookup(final String key) {
        return formatDate(System.currentTimeMillis(), key);
    }

    /**
     * Looks up d the current date or the date in the LogEvent.
     * @param event The LogEvent for which the date is returned. If null, current date is returned.
     * @param key the format to use. If null, the default DateFormat will be used.
     * @return The formatted date, never null.
     */
    @Override
    public String lookup(final LogEvent event, final String key) {
        return event == null ? lookup(key) : formatDate(event.getTimeMillis(), key);
    }

    private String formatDate(final long date, final String format) {
        DateFormat dateFormat = null;
        if (format != null) {
            try {
                dateFormat = new SimpleDateFormat(format);
            } catch (final Exception ex) {
                LOGGER.error(LOOKUP, "Invalid date format: [{}], using default", format, ex);
            }
        }
        if (dateFormat == null) {
            dateFormat = DateFormat.getInstance();
        }
        return dateFormat.format(new Date(date));
    }
}
