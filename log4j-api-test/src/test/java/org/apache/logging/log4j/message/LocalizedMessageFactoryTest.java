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
package org.apache.logging.log4j.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link LocalizedMessageFactory}.
 */
public class LocalizedMessageFactoryTest {

    @Test
    public void testMessageMarkersDataNo() {
        final LocalizedMessageFactory localizedMessageFactory =
                new LocalizedMessageFactory(ResourceBundle.getBundle("MF", Locale.US));
        final Message message = localizedMessageFactory.newMessage("msg1");
        assertEquals("This is test number {0} with string argument {1}.", message.getFormattedMessage());
    }

    @Test
    public void testMessageMarkersNoDataYes() {
        final LocalizedMessageFactory localizedMessageFactory =
                new LocalizedMessageFactory(ResourceBundle.getBundle("MF", Locale.US));
        final Message message = localizedMessageFactory.newMessage("msg1", 1, "two");
        assertEquals("This is test number 1 with string argument two.", message.getFormattedMessage());
    }

    @Test
    public void testNewMessage() {
        final LocalizedMessageFactory localizedMessageFactory =
                new LocalizedMessageFactory(ResourceBundle.getBundle("MF", Locale.US));
        final Message message = localizedMessageFactory.newMessage("hello_world");
        assertEquals("Hello world.", message.getFormattedMessage());
    }

    @Test
    public void testNoMatch() {
        final LocalizedMessageFactory localizedMessageFactory =
                new LocalizedMessageFactory(ResourceBundle.getBundle("MF", Locale.US));
        final Message message = localizedMessageFactory.newMessage("no match");
        assertEquals("no match", message.getFormattedMessage());
    }

    @Test
    public void testNoMatchPercentInMessageNoArgsNo() {
        // LOG4J2-3458 LocalizedMessage causes a lot of noise on the console
        //
        // ERROR StatusLogger Unable to format msg: C:/Program%20Files/Some%20Company/Some%20Product%20Name/
        // java.util.UnknownFormatConversionException: Conversion = 'F'
        // at java.util.Formatter$FormatSpecifier.conversion(Formatter.java:2691)
        // at java.util.Formatter$FormatSpecifier.<init>(Formatter.java:2720)
        // at java.util.Formatter.parse(Formatter.java:2560)
        // at java.util.Formatter.format(Formatter.java:2501)
        // at java.util.Formatter.format(Formatter.java:2455)
        // at java.lang.String.format(String.java:2981)
        // at org.apache.logging.log4j.message.StringFormattedMessage.formatMessage(StringFormattedMessage.java:116)
        // at
        // org.apache.logging.log4j.message.StringFormattedMessage.getFormattedMessage(StringFormattedMessage.java:88)
        // at org.apache.logging.log4j.message.FormattedMessage.getFormattedMessage(FormattedMessage.java:178)
        // at org.apache.logging.log4j.message.LocalizedMessage.getFormattedMessage(LocalizedMessage.java:196)
        // at
        // org.apache.logging.log4j.message.LocalizedMessageFactoryTest.testNoMatchPercentInMessage(LocalizedMessageFactoryTest.java:60)
        //
        final LocalizedMessageFactory localizedMessageFactory =
                new LocalizedMessageFactory(ResourceBundle.getBundle("MF", Locale.US));
        final Message message =
                localizedMessageFactory.newMessage("C:/Program%20Files/Some%20Company/Some%20Product%20Name/");
        assertEquals("C:/Program%20Files/Some%20Company/Some%20Product%20Name/", message.getFormattedMessage());
    }

    @Test
    public void testNoMatchPercentInMessageArgsYes() {
        final LocalizedMessageFactory localizedMessageFactory =
                new LocalizedMessageFactory(ResourceBundle.getBundle("MF", Locale.US));
        final Message message = localizedMessageFactory.newMessage(
                "C:/Program%20Files/Some%20Company/Some%20Product%20Name/{0}", "One");
        assertEquals("C:/Program%20Files/Some%20Company/Some%20Product%20Name/One", message.getFormattedMessage());
    }
}
