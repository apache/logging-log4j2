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
package org.apache.logging.log4j.trustgate.spi;

import org.apache.logging.log4j.trustgate.ValidationResult;

/**
 * Validates untrusted input before it is consumed by Log4j components.
 */
public interface InputSanitizer {

    /**
     * Validates the given input in the specified context.
     *
     * @param input the input to validate
     * @param type the input context
     * @return the validation outcome
     */
    ValidationResult validate(String input, InputType type);

    /**
     * Returns {@code true} when strict validation is enabled.
     *
     * @return {@code true} when validation rules are enforced
     */
    boolean isEnabled();
}
