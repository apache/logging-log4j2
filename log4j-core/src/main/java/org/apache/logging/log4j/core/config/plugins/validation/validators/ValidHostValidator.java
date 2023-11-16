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
package org.apache.logging.log4j.core.config.plugins.validation.validators;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Validator that checks an object to verify it is a valid hostname or IP address. Validation rules follow the same
 * logic as in {@link InetAddress#getByName(String)}.
 *
 * @since 2.8
 */
public class ValidHostValidator implements ConstraintValidator<ValidHost> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private ValidHost annotation;

    @Override
    public void initialize(final ValidHost annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(final String name, final Object value) {
        if (value == null) {
            LOGGER.error(annotation.message());
            return false;
        }
        if (value instanceof InetAddress) {
            // InetAddress factory methods all have built in validation
            return true;
        }
        try {
            InetAddress.getByName(value.toString());
            return true;
        } catch (final UnknownHostException e) {
            LOGGER.error(annotation.message(), e);
            return false;
        }
    }
}
