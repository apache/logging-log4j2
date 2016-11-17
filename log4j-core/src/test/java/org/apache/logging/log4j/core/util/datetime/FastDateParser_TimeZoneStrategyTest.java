/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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
package org.apache.logging.log4j.core.util.datetime;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;

/**
 * Copied from Apache Commons Lang 3 on 2016-11-16.
 */
public class FastDateParser_TimeZoneStrategyTest {

    @Test
    public void testTimeZoneStrategyPattern() {
        for(final Locale locale : Locale.getAvailableLocales()) {
            final FastDateParser parser = new FastDateParser("z", TimeZone.getDefault(), locale);
            final String[][] zones = DateFormatSymbols.getInstance(locale).getZoneStrings();
            for(final String[] zone :  zones) {
                for(int t = 1; t<zone.length; ++t) {
                    final String tzDisplay = zone[t];
                    if (tzDisplay == null) {
                        break;
                    }
                    try {
                        parser.parse(tzDisplay);
                    }
                    catch(final Exception ex) {
                        Assert.fail("'" + tzDisplay + "'"
                                + " Locale: '" + locale.getDisplayName() + "'"
                                + " TimeZone: " + zone[0]
                                + " offset: " + t
                                + " defaultLocale: " + Locale.getDefault()
                                + " defaultTimeZone: " + TimeZone.getDefault().getDisplayName()
                                );
                    }
                }
            }
        }
    }

    @Test
    public void testLang1219() throws ParseException {
        final FastDateParser parser = new FastDateParser("dd.MM.yyyy HH:mm:ss z", TimeZone.getDefault(), Locale.GERMAN);

        final Date summer = parser.parse("26.10.2014 02:00:00 MESZ");
        final Date standard = parser.parse("26.10.2014 02:00:00 MEZ");
        Assert.assertNotEquals(summer.getTime(), standard.getTime());
    }
}
