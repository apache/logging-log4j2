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
package org.apache.log4j.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class Log4j1SyslogLayoutTest {

    private static final SimpleMessage MESSAGE = new SimpleMessage("Hello world!");
    private static final long TIMESTAMP = LocalDateTime.of(2022, 4, 5, 12, 34, 56)
            .atZone(ZoneId.systemDefault())
            .toEpochSecond();
    private static final String localhostName = NetUtils.getLocalHostname();

    private static LogEvent createLogEvent() {
        final MutableInstant instant = new MutableInstant();
        instant.initFromEpochSecond(TIMESTAMP, 0);
        final LogEvent event = mock(LogEvent.class);
        when(event.getInstant()).thenReturn(instant);
        when(event.getMessage()).thenReturn(MESSAGE);
        when(event.getLevel()).thenReturn(Level.INFO);
        return event;
    }

    static Stream<Arguments> configurations() {
        return Stream.of(
                        Arguments.of("<30>Hello world!", Facility.DAEMON, false, false),
                        Arguments.of("<30>Apr  5 12:34:56 %s Hello world!", Facility.DAEMON, true, false),
                        Arguments.of("<30>daemon:Hello world!", Facility.DAEMON, false, true),
                        Arguments.of("<30>Apr  5 12:34:56 %s daemon:Hello world!", Facility.DAEMON, true, true))
                .map(args -> {
                    final Object[] objs = args.get();
                    objs[0] = String.format((String) objs[0], localhostName);
                    return Arguments.of(objs);
                });
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void testSimpleLayout(
            final String expected, final Facility facility, final boolean header, final boolean facilityPrinting) {
        final LogEvent logEvent = createLogEvent();
        StringLayout appenderLayout = Log4j1SyslogLayout.newBuilder()
                .setFacility(facility)
                .setHeader(header)
                .setFacilityPrinting(facilityPrinting)
                .build();
        assertEquals(expected, appenderLayout.toSerializable(logEvent));
        final StringLayout messageLayout =
                PatternLayout.newBuilder().withPattern("%m").build();
        appenderLayout = Log4j1SyslogLayout.newBuilder()
                .setFacility(facility)
                .setHeader(header)
                .setFacilityPrinting(facilityPrinting)
                .setMessageLayout(messageLayout)
                .build();
        assertEquals(expected, appenderLayout.toSerializable(logEvent));
    }
}
