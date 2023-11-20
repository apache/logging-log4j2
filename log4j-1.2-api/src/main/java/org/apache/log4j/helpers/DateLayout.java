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

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This abstract layout takes care of all the date related options and formatting work.
 */
public abstract class DateLayout extends Layout {

    /**
     * String constant designating no time information. Current value of this constant is <b>NULL</b>.
     *
     */
    public static final String NULL_DATE_FORMAT = "NULL";

    /**
     * String constant designating relative time. Current value of this constant is <b>RELATIVE</b>.
     */
    public static final String RELATIVE_TIME_DATE_FORMAT = "RELATIVE";

    /**
     * @deprecated Options are now handled using the JavaBeans paradigm. This constant is not longer needed and will be
     *             removed in the <em>near</em> term.
     */
    @Deprecated
    public static final String DATE_FORMAT_OPTION = "DateFormat";

    /**
     * @deprecated Options are now handled using the JavaBeans paradigm. This constant is not longer needed and will be
     *             removed in the <em>near</em> term.
     */
    @Deprecated
    public static final String TIMEZONE_OPTION = "TimeZone";

    protected FieldPosition pos = new FieldPosition(0);

    private String timeZoneID;
    private String dateFormatOption;

    protected DateFormat dateFormat;
    protected Date date = new Date();

    public void activateOptions() {
        setDateFormat(dateFormatOption);
        if (timeZoneID != null && dateFormat != null) {
            dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneID));
        }
    }

    public void dateFormat(final StringBuffer buf, final LoggingEvent event) {
        if (dateFormat != null) {
            date.setTime(event.timeStamp);
            dateFormat.format(date, buf, this.pos);
            buf.append(' ');
        }
    }

    /**
     * Returns value of the <b>DateFormat</b> option.
     */
    public String getDateFormat() {
        return dateFormatOption;
    }

    /**
     * @deprecated Use the setter method for the option directly instead of the generic <code>setOption</code> method.
     */
    @Deprecated
    public String[] getOptionStrings() {
        return new String[] {DATE_FORMAT_OPTION, TIMEZONE_OPTION};
    }

    /**
     * Returns value of the <b>TimeZone</b> option.
     */
    public String getTimeZone() {
        return timeZoneID;
    }

    /**
     * Sets the {@link DateFormat} used to format time and date in the zone determined by <code>timeZone</code>.
     */
    public void setDateFormat(final DateFormat dateFormat, final TimeZone timeZone) {
        this.dateFormat = dateFormat;
        this.dateFormat.setTimeZone(timeZone);
    }

    /**
     * The value of the <b>DateFormat</b> option should be either an argument to the constructor of {@link SimpleDateFormat}
     * or one of the srings "NULL", "RELATIVE", "ABSOLUTE", "DATE" or "ISO8601.
     */
    public void setDateFormat(final String dateFormat) {
        if (dateFormat != null) {
            dateFormatOption = dateFormat;
        }
        setDateFormat(dateFormatOption, TimeZone.getDefault());
    }

    /**
     * Sets the DateFormat used to format date and time in the time zone determined by <code>timeZone</code> parameter. The
     * {@link DateFormat} used will depend on the <code>dateFormatType</code>.
     *
     * <p>
     * The recognized types are {@link #NULL_DATE_FORMAT}, {@link #RELATIVE_TIME_DATE_FORMAT}
     * {@link AbsoluteTimeDateFormat#ABS_TIME_DATE_FORMAT}, {@link AbsoluteTimeDateFormat#DATE_AND_TIME_DATE_FORMAT} and
     * {@link AbsoluteTimeDateFormat#ISO8601_DATE_FORMAT}. If the <code>dateFormatType</code> is not one of the above, then
     * the argument is assumed to be a date pattern for {@link SimpleDateFormat}.
     */
    public void setDateFormat(final String dateFormatType, final TimeZone timeZone) {
        if (dateFormatType == null) {
            this.dateFormat = null;
            return;
        }
        if (dateFormatType.equalsIgnoreCase(NULL_DATE_FORMAT)) {
            this.dateFormat = null;
        } else if (dateFormatType.equalsIgnoreCase(RELATIVE_TIME_DATE_FORMAT)) {
            this.dateFormat = new RelativeTimeDateFormat();
        } else if (dateFormatType.equalsIgnoreCase(AbsoluteTimeDateFormat.ABS_TIME_DATE_FORMAT)) {
            this.dateFormat = new AbsoluteTimeDateFormat(timeZone);
        } else if (dateFormatType.equalsIgnoreCase(AbsoluteTimeDateFormat.DATE_AND_TIME_DATE_FORMAT)) {
            this.dateFormat = new DateTimeDateFormat(timeZone);
        } else if (dateFormatType.equalsIgnoreCase(AbsoluteTimeDateFormat.ISO8601_DATE_FORMAT)) {
            this.dateFormat = new ISO8601DateFormat(timeZone);
        } else {
            this.dateFormat = new SimpleDateFormat(dateFormatType);
            this.dateFormat.setTimeZone(timeZone);
        }
    }

    /**
     * @deprecated Use the setter method for the option directly instead of the generic <code>setOption</code> method.
     */
    @Deprecated
    public void setOption(final String option, final String value) {
        if (option.equalsIgnoreCase(DATE_FORMAT_OPTION)) {
            dateFormatOption = toRootUpperCase(value);
        } else if (option.equalsIgnoreCase(TIMEZONE_OPTION)) {
            timeZoneID = value;
        }
    }

    /**
     * The <b>TimeZoneID</b> option is a time zone ID string in the format expected by the {@link TimeZone#getTimeZone}
     * method.
     */
    public void setTimeZone(final String timeZone) {
        this.timeZoneID = timeZone;
    }
}
