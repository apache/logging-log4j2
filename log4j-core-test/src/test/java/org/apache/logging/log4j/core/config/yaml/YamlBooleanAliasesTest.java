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
package org.apache.logging.log4j.core.config.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.core.config.YamlBooleanAliases;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class YamlBooleanAliasesTest {

    @CsvSource({
        "y,true",
        "Y,true",
        "yes,true",
        "Yes,true",
        "YES,true",
        "on,true",
        "On,true",
        "ON,true",
        "n,false",
        "N,false",
        "no,false",
        "No,false",
        "NO,false",
        "off,false",
        "Off,false",
        "OFF,false"
    })
    @ParameterizedTest
    void normalizesYaml11BooleanAliases(final String value, final String expected) {
        assertThat(YamlBooleanAliases.normalize(value)).isEqualTo(expected);
    }

    @Test
    void leavesNonAliasesUnchanged() {
        assertThat(YamlBooleanAliases.normalize("true")).isEqualTo("true");
        assertThat(YamlBooleanAliases.normalize("text")).isEqualTo("text");
        assertThat(YamlBooleanAliases.normalize(null)).isNull();
    }
}
