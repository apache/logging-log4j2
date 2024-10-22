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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.jul.tolog4j.LevelTranslator;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JulLevelPropagatorTest {

    @BeforeAll
    static void setup() {
        // Ensure that at least one message was sent.
        Logger.getGlobal().info("Initialize");
    }

    @AfterEach
    void cleanup() {
        org.apache.logging.log4j.core.LoggerContext.getContext(false).reconfigure();
    }

    @Test
    void initial_synchronization_works() {
        // JUL levels are set from config files and the initial propagation
        assertThat(Logger.getLogger("").getLevel()).isEqualTo(Level.FINER);
        assertThat(Logger.getLogger("foo").getLevel()).isEqualTo(Level.WARNING);
        assertThat(Logger.getLogger("foo.bar").getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void synchronization_retained_after_GC() {
        initial_synchronization_works();
        System.gc(); // a single call is sufficient
        initial_synchronization_works();
    }

    @Test
    void when_set_level_synchronization_works() {
        Configurator.setLevel("", LevelTranslator.CONFIG);
        Configurator.setLevel("foo", org.apache.logging.log4j.Level.DEBUG);
        Configurator.setLevel("foo.bar", org.apache.logging.log4j.Level.TRACE);

        assertThat(Logger.getLogger("").getLevel()).isEqualTo(Level.CONFIG);
        assertThat(Logger.getLogger("foo").getLevel()).isEqualTo(Level.FINE);
        assertThat(Logger.getLogger("foo.bar").getLevel()).isEqualTo(Level.FINER);
    }
}
