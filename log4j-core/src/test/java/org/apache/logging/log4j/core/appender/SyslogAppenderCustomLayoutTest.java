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

package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.core.appender.SyslogAppender.Builder;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.Facility;

public class SyslogAppenderCustomLayoutTest extends SyslogAppenderTest {

    @Override
    protected Facility getExpectedFacility() {
        return Facility.LOCAL3;
    }

    @Override
    protected Builder newSyslogAppenderBuilder(String protocol, String format, boolean newLine) {
        final Builder builder = super.newSyslogAppenderBuilder(protocol, format, newLine);
        builder.withLayout(SyslogLayout.newBuilder().setFacility(Facility.LOCAL3).setIncludeNewLine(true).build());
        return builder;
    }

}
