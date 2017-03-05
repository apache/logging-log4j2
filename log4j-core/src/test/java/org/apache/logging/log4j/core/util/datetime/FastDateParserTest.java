/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional inparserion regarding copyright ownership.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests {@link org.apache.commons.lang3.time.FastDateParser}.
 * 
 * Copied from Apache Commons Lang 3 on 2016-11-16.
 */
public class FastDateParserTest {
    private static final String SHORT_FORMAT_NOERA = "y/M/d/h/a/m/s/E";
    private static final String LONG_FORMAT_NOERA = "yyyy/MMMM/dddd/hhhh/mmmm/ss/aaaa/EEEE";
    private static final String SHORT_FORMAT = "G/" + SHORT_FORMAT_NOERA;
    private static final String LONG_FORMAT = "GGGG/" + LONG_FORMAT_NOERA;

    private static final String yMdHmsSZ = "yyyy-MM-dd'T'HH:mm:ss.SSS Z";
    private static final String DMY_DOT = "dd.MM.yyyy";
    private static final String YMD_SLASH = "yyyy/MM/dd";
    private static final String MDY_DASH = "MM-DD-yyyy";
    private static final String MDY_SLASH = "MM/DD/yyyy";

    private static final TimeZone REYKJAVIK = TimeZone.getTimeZone("Atlantic/Reykjavik");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    private static final TimeZone INDIA = TimeZone.getTimeZone("Asia/Calcutta");

    private static final Locale SWEDEN = new Locale("sv", "SE");

    DateParser getInstance(final String format) {
        return getInstance(format, TimeZone.getDefault(), Locale.getDefault());
    }

    private DateParser getDateInstance(final int dateStyle, final Locale locale) {
        return getInstance(FormatCache.getPatternForStyle(Integer.valueOf(dateStyle), null, locale), TimeZone.getDefault(), Locale.getDefault());
    }

    private DateParser getInstance(final String format, final Locale locale) {
        return getInstance(format, TimeZone.getDefault(), locale);
    }

    private DateParser getInstance(final String format, final TimeZone timeZone) {
        return getInstance(format, timeZone, Locale.getDefault());
    }

    /**
     * Override this method in derived tests to change the construction of instances
     *
     * @param format the format string to use
     * @param timeZone the time zone to use
     * @param locale the locale to use
     *
     * @return the DateParser instance to use for testing
     */
    protected DateParser getInstance(final String format, final TimeZone timeZone, final Locale locale) {
        return new FastDateParser(format, timeZone, locale, null);
    }

    @Test
    public void test_Equality_Hash() {
        final DateParser[] parsers= {
            getInstance(yMdHmsSZ, NEW_YORK, Locale.US),
            getInstance(DMY_DOT, NEW_YORK, Locale.US),
            getInstance(YMD_SLASH, NEW_YORK, Locale.US),
            getInstance(MDY_DASH, NEW_YORK, Locale.US),
            getInstance(MDY_SLASH, NEW_YORK, Locale.US),
            getInstance(MDY_SLASH, REYKJAVIK, Locale.US),
            getInstance(MDY_SLASH, REYKJAVIK, SWEDEN)
        };

        final Map<DateParser,Integer> map= new HashMap<>();
        int i= 0;
        for(final DateParser parser:parsers) {
            map.put(parser, Integer.valueOf(i++));
        }

        i= 0;
        for(final DateParser parser:parsers) {
            assertEquals(i++, map.get(parser).intValue());
        }
    }

    @Test
    public void testParseZone() throws ParseException {
        final Calendar cal= Calendar.getInstance(NEW_YORK, Locale.US);
        cal.clear();
        cal.set(2003, Calendar.JULY, 10, 16, 33, 20);

        final DateParser fdf = getInstance(yMdHmsSZ, NEW_YORK, Locale.US);

        assertEquals(cal.getTime(), fdf.parse("2003-07-10T15:33:20.000 -0500"));
        assertEquals(cal.getTime(), fdf.parse("2003-07-10T15:33:20.000 GMT-05:00"));
        assertEquals(cal.getTime(), fdf.parse("2003-07-10T16:33:20.000 Eastern Daylight Time"));
        assertEquals(cal.getTime(), fdf.parse("2003-07-10T16:33:20.000 EDT"));

        cal.setTimeZone(TimeZone.getTimeZone("GMT-3"));
        cal.set(2003, Calendar.FEBRUARY, 10, 9, 0, 0);

        assertEquals(cal.getTime(), fdf.parse("2003-02-10T09:00:00.000 -0300"));

        cal.setTimeZone(TimeZone.getTimeZone("GMT+5"));
        cal.set(2003, Calendar.FEBRUARY, 10, 15, 5, 6);

        assertEquals(cal.getTime(), fdf.parse("2003-02-10T15:05:06.000 +0500"));
    }

    @Test
    public void testParseLongShort() throws ParseException {
        final Calendar cal= Calendar.getInstance(NEW_YORK, Locale.US);
        cal.clear();
        cal.set(2003, Calendar.FEBRUARY, 10, 15, 33, 20);
        cal.set(Calendar.MILLISECOND, 989);
        cal.setTimeZone(NEW_YORK);

        DateParser fdf = getInstance("yyyy GGGG MMMM dddd aaaa EEEE HHHH mmmm ssss SSSS ZZZZ", NEW_YORK, Locale.US);

        assertEquals(cal.getTime(), fdf.parse("2003 AD February 0010 PM Monday 0015 0033 0020 0989 GMT-05:00"));
        cal.set(Calendar.ERA, GregorianCalendar.BC);

        final Date parse = fdf.parse("2003 BC February 0010 PM Saturday 0015 0033 0020 0989 GMT-05:00");
                assertEquals(cal.getTime(), parse);

        fdf = getInstance("y G M d a E H m s S Z", NEW_YORK, Locale.US);
        assertEquals(cal.getTime(), fdf.parse("03 BC 2 10 PM Sat 15 33 20 989 -0500"));

        cal.set(Calendar.ERA, GregorianCalendar.AD);
        assertEquals(cal.getTime(), fdf.parse("03 AD 2 10 PM Saturday 15 33 20 989 -0500"));
    }

    @Test
    public void testAmPm() throws ParseException {
        final Calendar cal= Calendar.getInstance(NEW_YORK, Locale.US);
        cal.clear();

        final DateParser h = getInstance("yyyy-MM-dd hh a mm:ss", NEW_YORK, Locale.US);
        final DateParser K = getInstance("yyyy-MM-dd KK a mm:ss", NEW_YORK, Locale.US);
        final DateParser k = getInstance("yyyy-MM-dd kk:mm:ss", NEW_YORK, Locale.US);
        final DateParser H = getInstance("yyyy-MM-dd HH:mm:ss", NEW_YORK, Locale.US);

        cal.set(2010, Calendar.AUGUST, 1, 0, 33, 20);
        assertEquals(cal.getTime(), h.parse("2010-08-01 12 AM 33:20"));
        assertEquals(cal.getTime(), K.parse("2010-08-01 0 AM 33:20"));
        assertEquals(cal.getTime(), k.parse("2010-08-01 00:33:20"));
        assertEquals(cal.getTime(), H.parse("2010-08-01 00:33:20"));

        cal.set(2010, Calendar.AUGUST, 1, 3, 33, 20);
        assertEquals(cal.getTime(), h.parse("2010-08-01 3 AM 33:20"));
        assertEquals(cal.getTime(), K.parse("2010-08-01 3 AM 33:20"));
        assertEquals(cal.getTime(), k.parse("2010-08-01 03:33:20"));
        assertEquals(cal.getTime(), H.parse("2010-08-01 03:33:20"));

        cal.set(2010, Calendar.AUGUST, 1, 15, 33, 20);
        assertEquals(cal.getTime(), h.parse("2010-08-01 3 PM 33:20"));
        assertEquals(cal.getTime(), K.parse("2010-08-01 3 PM 33:20"));
        assertEquals(cal.getTime(), k.parse("2010-08-01 15:33:20"));
        assertEquals(cal.getTime(), H.parse("2010-08-01 15:33:20"));

        cal.set(2010, Calendar.AUGUST, 1, 12, 33, 20);
        assertEquals(cal.getTime(), h.parse("2010-08-01 12 PM 33:20"));
        assertEquals(cal.getTime(), K.parse("2010-08-01 0 PM 33:20"));
        assertEquals(cal.getTime(), k.parse("2010-08-01 12:33:20"));
        assertEquals(cal.getTime(), H.parse("2010-08-01 12:33:20"));
    }

    private Calendar getEraStart(int year, final TimeZone zone, final Locale locale) {
        final Calendar cal = Calendar.getInstance(zone, locale);
        cal.clear();

        // http://docs.oracle.com/javase/6/docs/technotes/guides/intl/calendar.doc.html
        if (locale.equals(FastDateParser.JAPANESE_IMPERIAL)) {
            if(year < 1868) {
                cal.set(Calendar.ERA, 0);
                cal.set(Calendar.YEAR, 1868-year);
            }
        }
        else {
            if (year < 0) {
                cal.set(Calendar.ERA, GregorianCalendar.BC);
                year= -year;
            }
            cal.set(Calendar.YEAR, year/100 * 100);
        }
        return cal;
    }

    private void validateSdfFormatFdpParseEquality(final String format, final Locale locale, final TimeZone tz, final DateParser fdp, final Date in, final int year, final Date cs) throws ParseException {
        final SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
        sdf.setTimeZone(tz);
        if (format.equals(SHORT_FORMAT)) {
            sdf.set2DigitYearStart( cs );
        }
        final String fmt = sdf.format(in);
        try {
            final Date out = fdp.parse(fmt);
            assertEquals(locale.toString()+" "+in+" "+ format+ " "+tz.getID(), in, out);
        } catch (final ParseException pe) {
            if (year >= 1868 || !locale.getCountry().equals("JP")) {// LANG-978
                throw pe;
            }
        }
    }

    @Test
    // Check that all Locales can parse the formats we use
    public void testParses() throws Exception {
        for(final String format : new String[]{LONG_FORMAT, SHORT_FORMAT}) {
            for(final Locale locale : Locale.getAvailableLocales()) {
                for(final TimeZone tz :  new TimeZone[]{NEW_YORK, REYKJAVIK, GMT}) {
                     for(final int year : new int[]{2003, 1940, 1868, 1867, 1, -1, -1940}) {
                        final Calendar cal= getEraStart(year, tz, locale);
                        final Date centuryStart= cal.getTime();

                        cal.set(Calendar.MONTH, 1);
                        cal.set(Calendar.DAY_OF_MONTH, 10);
                        final Date in= cal.getTime();

                        final FastDateParser fdp= new FastDateParser(format, tz, locale, centuryStart);
                        validateSdfFormatFdpParseEquality(format, locale, tz, fdp, in, year, centuryStart);
                    }
                }
            }
        }
    }

    // we cannot use historic dates to test timezone parsing, some timezones have second offsets
    // as well as hours and minutes which makes the z formats a low fidelity round trip
    @Test
    public void testTzParses() throws Exception {
        // Check that all Locales can parse the time formats we use
        for(final Locale locale : Locale.getAvailableLocales()) {
            final FastDateParser fdp= new FastDateParser("yyyy/MM/dd z", TimeZone.getDefault(), locale);

            for(final TimeZone tz :  new TimeZone[]{NEW_YORK, REYKJAVIK, GMT}) {
                final Calendar cal= Calendar.getInstance(tz, locale);
                cal.clear();
                cal.set(Calendar.YEAR, 2000);
                cal.set(Calendar.MONTH, 1);
                cal.set(Calendar.DAY_OF_MONTH, 10);
                final Date expected= cal.getTime();

                final Date actual = fdp.parse("2000/02/10 "+tz.getDisplayName(locale));
                Assert.assertEquals("tz:"+tz.getID()+" locale:"+locale.getDisplayName(), expected, actual);
            }
        }
    }


    @Test
    public void testLocales_Long_AD() throws Exception {
        testLocales(LONG_FORMAT, false);
    }

    @Test
    public void testLocales_Long_BC() throws Exception {
        testLocales(LONG_FORMAT, true);
    }

    @Test
    public void testLocales_Short_AD() throws Exception {
        testLocales(SHORT_FORMAT, false);
    }

    @Test
    public void testLocales_Short_BC() throws Exception {
        testLocales(SHORT_FORMAT, true);
    }

    @Test
    public void testLocales_LongNoEra_AD() throws Exception {
        testLocales(LONG_FORMAT_NOERA, false);
    }

    @Test
    public void testLocales_LongNoEra_BC() throws Exception {
        testLocales(LONG_FORMAT_NOERA, true);
    }

    @Test
    public void testLocales_ShortNoEra_AD() throws Exception {
        testLocales(SHORT_FORMAT_NOERA, false);
    }

    @Test
    public void testLocales_ShortNoEra_BC() throws Exception {
        testLocales(SHORT_FORMAT_NOERA, true);
    }

    private void testLocales(final String format, final boolean eraBC) throws Exception {

        final Calendar cal= Calendar.getInstance(GMT);
        cal.clear();
        cal.set(2003, Calendar.FEBRUARY, 10);
        if (eraBC) {
            cal.set(Calendar.ERA, GregorianCalendar.BC);
        }

        for(final Locale locale : Locale.getAvailableLocales() ) {
            // ja_JP_JP cannot handle dates before 1868 properly
            if (eraBC && locale.equals(FastDateParser.JAPANESE_IMPERIAL)) {
                continue;
            }
            final SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
            final DateParser fdf = getInstance(format, locale);

            try {
                checkParse(locale, cal, sdf, fdf);
            } catch(final ParseException ex) {
                Assert.fail("Locale "+locale+ " failed with "+format+" era "+(eraBC?"BC":"AD")+"\n" + trimMessage(ex.toString()));
            }
        }
    }
    
    @Test
    public void testJpLocales() {

        final Calendar cal= Calendar.getInstance(GMT);
        cal.clear();
        cal.set(2003, Calendar.FEBRUARY, 10);
        cal.set(Calendar.ERA, GregorianCalendar.BC);

        final Locale locale = LocaleUtils.toLocale("zh"); {
            // ja_JP_JP cannot handle dates before 1868 properly

            final SimpleDateFormat sdf = new SimpleDateFormat(LONG_FORMAT, locale);
            final DateParser fdf = getInstance(LONG_FORMAT, locale);

            try {
                checkParse(locale, cal, sdf, fdf);
            } catch(final ParseException ex) {
                Assert.fail("Locale "+locale+ " failed with "+LONG_FORMAT+"\n" + trimMessage(ex.toString()));
            }
        }
    }

    private String trimMessage(final String msg) {
        if (msg.length() < 100) {
            return msg;
        }
        final int gmt = msg.indexOf("(GMT");
        if (gmt > 0) {
            return msg.substring(0, gmt+4)+"...)";
        }
        return msg.substring(0, 100)+"...";
    }

    private void checkParse(final Locale locale, final Calendar cal, final SimpleDateFormat sdf, final DateParser fdf) throws ParseException {
        final String formattedDate= sdf.format(cal.getTime());
        checkParse(locale, sdf, fdf, formattedDate);
        checkParse(locale, sdf, fdf, formattedDate.toLowerCase(locale));
        checkParse(locale, sdf, fdf, formattedDate.toUpperCase(locale));
    }

    private void checkParse(final Locale locale, final SimpleDateFormat sdf, final DateParser fdf, final String formattedDate) throws ParseException {
        final Date expectedTime = sdf.parse(formattedDate);
        final Date actualTime = fdf.parse(formattedDate);
        assertEquals(locale.toString()+" "+formattedDate +"\n",expectedTime, actualTime);
    }

    @Test
    public void testParseNumerics() throws ParseException {
        final Calendar cal= Calendar.getInstance(NEW_YORK, Locale.US);
        cal.clear();
        cal.set(2003, Calendar.FEBRUARY, 10, 15, 33, 20);
        cal.set(Calendar.MILLISECOND, 989);

        final DateParser fdf = getInstance("yyyyMMddHHmmssSSS", NEW_YORK, Locale.US);
        assertEquals(cal.getTime(), fdf.parse("20030210153320989"));
    }

    @Test
    public void testQuotes() throws ParseException {
        final Calendar cal= Calendar.getInstance(NEW_YORK, Locale.US);
        cal.clear();
        cal.set(2003, Calendar.FEBRUARY, 10, 15, 33, 20);
        cal.set(Calendar.MILLISECOND, 989);

        final DateParser fdf = getInstance("''yyyyMMdd'A''B'HHmmssSSS''", NEW_YORK, Locale.US);
        assertEquals(cal.getTime(), fdf.parse("'20030210A'B153320989'"));
    }

    @Test
    public void testSpecialCharacters() throws Exception {
        testSdfAndFdp("q" ,"", true); // bad pattern character (at present)
        testSdfAndFdp("Q" ,"", true); // bad pattern character
        testSdfAndFdp("$" ,"$", false); // OK
        testSdfAndFdp("?.d" ,"?.12", false); // OK
        testSdfAndFdp("''yyyyMMdd'A''B'HHmmssSSS''", "'20030210A'B153320989'", false); // OK
        testSdfAndFdp("''''yyyyMMdd'A''B'HHmmssSSS''", "''20030210A'B153320989'", false); // OK
        testSdfAndFdp("'$\\Ed'" ,"$\\Ed", false); // OK
        
        // quoted charaters are case sensitive
        testSdfAndFdp("'QED'", "QED", false);
        testSdfAndFdp("'QED'", "qed", true);
        // case sensitive after insensitive Month field
        testSdfAndFdp("yyyy-MM-dd 'QED'", "2003-02-10 QED", false);
        testSdfAndFdp("yyyy-MM-dd 'QED'", "2003-02-10 qed", true);
    }
    
    @Test
    public void testLANG_832() throws Exception {
        testSdfAndFdp("'d'd" ,"d3", false); // OK
        testSdfAndFdp("'d'd'","d3", true); // should fail (unterminated quote)
    }

    @Test
    public void testLANG_831() throws Exception {
        testSdfAndFdp("M E","3  Tue", true);
    }

    private void testSdfAndFdp(final String format, final String date, final boolean shouldFail)
            throws Exception {
        Date dfdp = null;
        Date dsdf = null;
        Throwable f = null;
        Throwable s = null;

        try {
            final SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
            sdf.setTimeZone(NEW_YORK);
            dsdf = sdf.parse(date);
            if (shouldFail) {
                Assert.fail("Expected SDF failure, but got " + dsdf + " for ["+format+","+date+"]");
            }
        } catch (final Exception e) {
            s = e;
            if (!shouldFail) {
                throw e;
            }
        }

        try {
            final DateParser fdp = getInstance(format, NEW_YORK, Locale.US);
            dfdp = fdp.parse(date);
            if (shouldFail) {
                Assert.fail("Expected FDF failure, but got " + dfdp + " for ["+format+","+date+"]");
            }
        } catch (final Exception e) {
            f = e;
            if (!shouldFail) {
                throw e;
            }
        }
        // SDF and FDF should produce equivalent results
        assertTrue("Should both or neither throw Exceptions", (f==null)==(s==null));
        assertEquals("Parsed dates should be equal", dsdf, dfdp);
    }

    @Test
    public void testDayOf() throws ParseException {
        final Calendar cal= Calendar.getInstance(NEW_YORK, Locale.US);
        cal.clear();
        cal.set(2003, Calendar.FEBRUARY, 10);

        final DateParser fdf = getInstance("W w F D y", NEW_YORK, Locale.US);
        assertEquals(cal.getTime(), fdf.parse("3 7 2 41 03"));
    }

    /**
     * Test case for {@link FastDateParser#FastDateParser(String, TimeZone, Locale)}.
     * @throws ParseException so we don't have to catch it
     */
    @Test
    public void testShortDateStyleWithLocales() throws ParseException {
        DateParser fdf = getDateInstance(FastDateFormat.SHORT, Locale.US);
        final Calendar cal = Calendar.getInstance();
        cal.clear();

        cal.set(2004, Calendar.FEBRUARY, 3);
        assertEquals(cal.getTime(), fdf.parse("2/3/04"));

        fdf = getDateInstance(FastDateFormat.SHORT, SWEDEN);
        assertEquals(cal.getTime(), fdf.parse("2004-02-03"));
    }

    /**
     * Tests that pre-1000AD years get padded with yyyy
     * @throws ParseException so we don't have to catch it
     */
    @Test
    public void testLowYearPadding() throws ParseException {
        final DateParser parser = getInstance(YMD_SLASH);
        final Calendar cal = Calendar.getInstance();
        cal.clear();

        cal.set(1, Calendar.JANUARY, 1);
        assertEquals(cal.getTime(), parser.parse("0001/01/01"));
        cal.set(10, Calendar.JANUARY, 1);
        assertEquals(cal.getTime(), parser.parse("0010/01/01"));
        cal.set(100, Calendar.JANUARY, 1);
        assertEquals(cal.getTime(), parser.parse("0100/01/01"));
        cal.set(999, Calendar.JANUARY, 1);
        assertEquals(cal.getTime(), parser.parse("0999/01/01"));
    }

    @Test
    public void testMilleniumBug() throws ParseException {
        final DateParser parser = getInstance(DMY_DOT);
        final Calendar cal = Calendar.getInstance();
        cal.clear();

        cal.set(1000, Calendar.JANUARY, 1);
        assertEquals(cal.getTime(), parser.parse("01.01.1000"));
    }

    @Test
    public void testLang303() throws ParseException {
        DateParser parser = getInstance(YMD_SLASH);
        final Calendar cal = Calendar.getInstance();
        cal.set(2004, Calendar.DECEMBER, 31);

        final Date date = parser.parse("2004/11/31");

        parser = SerializationUtils.deserialize(SerializationUtils.serialize((Serializable) parser));
        assertEquals(date, parser.parse("2004/11/31"));
    }

    @Test
    public void testLang538() throws ParseException {
        final DateParser parser = getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", GMT);

        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-8"));
        cal.clear();
        cal.set(2009, Calendar.OCTOBER, 16, 8, 42, 16);

        assertEquals(cal.getTime(), parser.parse("2009-10-16T16:42:16.000Z"));
    }

    @Test
    public void testEquals() {
        final DateParser parser1= getInstance(YMD_SLASH);
        final DateParser parser2= getInstance(YMD_SLASH);

        assertEquals(parser1, parser2);
        assertEquals(parser1.hashCode(), parser2.hashCode());

        assertFalse(parser1.equals(new Object()));
    }

    @Test
    public void testToStringContainsName() {
        final DateParser parser= getInstance(YMD_SLASH);
        assertTrue(parser.toString().startsWith("FastDate"));
    }

    @Test
    public void testPatternMatches() {
        final DateParser parser= getInstance(yMdHmsSZ);
        assertEquals(yMdHmsSZ, parser.getPattern());
    }

    @Test
    public void testLocaleMatches() {
        final DateParser parser= getInstance(yMdHmsSZ, SWEDEN);
        assertEquals(SWEDEN, parser.getLocale());
    }

    @Test
    public void testTimeZoneMatches() {
        final DateParser parser= getInstance(yMdHmsSZ, REYKJAVIK);
        assertEquals(REYKJAVIK, parser.getTimeZone());
    }
    
    @Test
    public void testLang996() throws ParseException {
        final Calendar expected = Calendar.getInstance(NEW_YORK, Locale.US);
        expected.clear();
        expected.set(2014, Calendar.MAY, 14);

        final DateParser fdp = getInstance("ddMMMyyyy", NEW_YORK, Locale.US);        
        assertEquals(expected.getTime(), fdp.parse("14may2014"));
        assertEquals(expected.getTime(), fdp.parse("14MAY2014"));
        assertEquals(expected.getTime(), fdp.parse("14May2014"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void test1806Argument() {
        getInstance("XXXX");
    }

    private static Calendar initializeCalendar(final TimeZone tz) {
        final Calendar cal = Calendar.getInstance(tz);
        cal.set(Calendar.YEAR, 2001);
        cal.set(Calendar.MONTH, 1); // not daylight savings
        cal.set(Calendar.DAY_OF_MONTH, 4);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 8);
        cal.set(Calendar.SECOND, 56);
        cal.set(Calendar.MILLISECOND, 235);
        return cal;
    }

    private static enum Expected1806 {
        India(INDIA, "+05", "+0530", "+05:30", true), 
        Greenwich(GMT, "Z", "Z", "Z", false), 
        NewYork(NEW_YORK, "-05", "-0500", "-05:00", false);

        private Expected1806(final TimeZone zone, final String one, final String two, final String three, final boolean hasHalfHourOffset) {
            this.zone = zone;
            this.one = one;
            this.two = two;
            this.three = three;
            this.offset = hasHalfHourOffset ?30*60*1000 :0;
        }

        final TimeZone zone;
        final String one;
        final String two;
        final String three;
        final long offset;
    }
    
    @Test
    public void test1806() throws ParseException {
        final String formatStub = "yyyy-MM-dd'T'HH:mm:ss.SSS";
        final String dateStub = "2001-02-04T12:08:56.235";
        
        for (final Expected1806 trial : Expected1806.values()) {
            final Calendar cal = initializeCalendar(trial.zone);

            final String message = trial.zone.getDisplayName()+";";
            
            DateParser parser = getInstance(formatStub+"X", trial.zone);
            assertEquals(message+trial.one, cal.getTime().getTime(), parser.parse(dateStub+trial.one).getTime()-trial.offset);

            parser = getInstance(formatStub+"XX", trial.zone);
            assertEquals(message+trial.two, cal.getTime(), parser.parse(dateStub+trial.two));

            parser = getInstance(formatStub+"XXX", trial.zone);
            assertEquals(message+trial.three, cal.getTime(), parser.parse(dateStub+trial.three));
        }
    }

    @Test
    public void testLang1121() throws ParseException {
        final TimeZone kst = TimeZone.getTimeZone("KST");
        final DateParser fdp = getInstance("yyyyMMdd", kst, Locale.KOREA);

        try {
            fdp.parse("2015");
            Assert.fail("expected parse exception");
        } catch (final ParseException pe) {
            // expected parse exception
        }

        // Wed Apr 29 00:00:00 KST 2015
        Date actual = fdp.parse("20150429");
        final Calendar cal = Calendar.getInstance(kst, Locale.KOREA);
        cal.clear();
        cal.set(2015, 3, 29);
        Date expected = cal.getTime();
        Assert.assertEquals(expected, actual);

        final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        df.setTimeZone(kst);
        expected = df.parse("20150429113100");

        // Thu Mar 16 00:00:00 KST 81724
        actual = fdp.parse("20150429113100");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testParseOffset() {
        final DateParser parser = getInstance(YMD_SLASH);
        final Date date = parser.parse("Today is 2015/07/04", new ParsePosition(9));

        final Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, Calendar.JULY, 4);
        Assert.assertEquals(cal.getTime(), date);
    }

    @Test
    public void testDayNumberOfWeek() throws ParseException {
        final DateParser parser = getInstance("u");
        final Calendar calendar = Calendar.getInstance();

        calendar.setTime(parser.parse("1"));
        Assert.assertEquals(Calendar.MONDAY, calendar.get(Calendar.DAY_OF_WEEK));

        calendar.setTime(parser.parse("6"));
        Assert.assertEquals(Calendar.SATURDAY, calendar.get(Calendar.DAY_OF_WEEK));

        calendar.setTime(parser.parse("7"));
        Assert.assertEquals(Calendar.SUNDAY, calendar.get(Calendar.DAY_OF_WEEK));
    }
}
