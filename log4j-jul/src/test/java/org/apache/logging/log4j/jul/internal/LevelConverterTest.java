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
package org.apache.logging.log4j.jul.internal;

import static org.apache.logging.log4j.jul.internal.LevelConverter.julToLog4jIntLevel;
import static org.apache.logging.log4j.jul.internal.LevelConverter.log4jToJulIntLevel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

class LevelConverterTest {

    @Test
    void intConversionsAreInverseFunctions() {
        for (int log4jLevel = Level.OFF.intLevel() + 1, expectedJulLevel = 1200;
                log4jLevel < Level.TRACE.intLevel() + 200;
                log4jLevel++) {
            // We decrement `expectedJulLevel` generally by 1,
            // but between INFO and DEBUG there are 100 Log4j levels, but 300 JUL levels,
            // so sometimes we decrement more.
            if (log4jLevel <= Level.INFO.intLevel()) {
                expectedJulLevel--;
            } else if (log4jLevel <= LevelConverter.CONFIG.intLevel()) {
                expectedJulLevel -= 2;
            } else if (log4jLevel <= Level.DEBUG.intLevel()) {
                expectedJulLevel -= 4;
            } else {
                expectedJulLevel--;
            }

            int julLevel = log4jToJulIntLevel(log4jLevel);

            assertThat(julLevel)
                    .as("JUL Level corresponding to Log4j Level %d", log4jLevel)
                    .isEqualTo(expectedJulLevel);
            assertThat(julToLog4jIntLevel(log4jToJulIntLevel(log4jLevel)))
                    .as("Log4j level %d", log4jLevel)
                    .isEqualTo(log4jLevel);
        }
    }

    /**
     * (LOG4J2-1108) NullPointerException when passing null to java.util.logging.Logger.setLevel().
     */
    @Test
    public void testJulSetNull() {
        assertNull(LevelConverter.toLog4jLevel(null));
    }
}
