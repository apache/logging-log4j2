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
package org.apache.logging.log4j;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

public class FormatterLoggerManualExample {

    private static class User {
        Calendar getBirthdayCalendar() {
            return new GregorianCalendar(1995, Calendar.MAY, 23);
        }

        String getName() {
            return "John Smith";
        }
    }

    public static Logger logger = LogManager.getFormatterLogger("Foo");

    /**
     * @param args
     */
    public static void main(final String[] args) {
        try (final LoggerContext ctx = Configurator.initialize(FormatterLoggerManualExample.class.getName(),
                "target/test-classes/log4j2-console.xml");) {
            final User user = new User();
            logger.debug("User %s with birthday %s", user.getName(), user.getBirthdayCalendar());
            logger.debug("User %1$s with birthday %2$tm %2$te, %2$tY", user.getName(), user.getBirthdayCalendar());
            logger.debug("Integer.MAX_VALUE = %,d", Integer.MAX_VALUE);
            logger.debug("Long.MAX_VALUE = %,d", Long.MAX_VALUE);
        }

    }

}
