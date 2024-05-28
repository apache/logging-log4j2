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
package org.apache.logging.log4j.jndi.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

/**
 * JndiDisabledLookupTest
 *
 * Verifies the Lookups are disabled without the log4j2.enableJndiLookup property set to true.
 */
@SetSystemProperty(key = "log4j2.status.entries", value = "10")
@SetSystemProperty(key = "log4j2.StatusLogger.level", value = "WARN")
class JndiDisabledLookupTest {

    @Test
    void testLookup() {
        assertThat(JndiLookup.createLookup()).isNull();
        assertThat(StatusLogger.getLogger().getStatusData()).anySatisfy(data -> {
            assertThat(data.getLevel()).isEqualTo(Level.ERROR);
            assertThat(data.getMessage().getFormattedMessage())
                    .isEqualTo(
                            "Ignoring request to use JNDI lookup. JNDI must be enabled by setting log4j.enableJndiLookup=true");
        });
    }
}
