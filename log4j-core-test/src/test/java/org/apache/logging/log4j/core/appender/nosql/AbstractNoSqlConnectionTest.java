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
package org.apache.logging.log4j.core.appender.nosql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AbstractNoSqlConnectionTest {

    @Test
    void loadsPatternRelativeToOwner() {
        final Pattern pattern = loadPattern("valid.properties");
        assertThat(pattern.matcher("reserved_42").matches()).isTrue();
        assertThat(pattern.matcher("prefix_reserved_42").matches()).isFalse();
        assertThat(pattern.matcher("reserved_42_suffix").matches()).isFalse();
    }

    @Test
    void rejectsMissingResource() {
        assertThatThrownBy(() -> loadPattern("missing.properties"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing reserved-field resource")
                .hasMessageContaining(AbstractNoSqlConnectionTest.class.getName())
                .hasMessageContaining("missing.properties");
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-pattern.properties", "empty-pattern.properties"})
    void rejectsMissingOrEmptyPattern(final String resourceName) {
        assertThatThrownBy(() -> loadPattern(resourceName))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing reservedFieldPattern")
                .hasMessageContaining(resourceName);
    }

    @Test
    void rejectsInvalidPattern() {
        assertThatThrownBy(() -> loadPattern("invalid.properties"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid.properties")
                .hasCauseInstanceOf(PatternSyntaxException.class);
    }

    private static Pattern loadPattern(final String resourceName) {
        return AbstractNoSqlConnection.loadReservedFieldPattern(
                AbstractNoSqlConnectionTest.class, "AbstractNoSqlConnectionTest/" + resourceName);
    }
}
