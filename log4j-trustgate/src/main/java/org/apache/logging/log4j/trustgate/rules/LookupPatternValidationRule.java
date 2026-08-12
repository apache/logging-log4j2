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
package org.apache.logging.log4j.trustgate.rules;

import org.apache.logging.log4j.trustgate.spi.InputType;
import org.apache.logging.log4j.trustgate.spi.ValidationRule;

/**
 * Rejects log messages that contain lookup substitution markers.
 */
public final class LookupPatternValidationRule implements ValidationRule {

    static final String LOOKUP_START = "${";
    private static final String RULE_NAME = "lookup-pattern";

    @Override
    public boolean matches(final String input, final InputType type) {
        if (type != InputType.LOG_MESSAGE) {
            return false;
        }
        return input.indexOf(LOOKUP_START) >= 0;
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }
}
