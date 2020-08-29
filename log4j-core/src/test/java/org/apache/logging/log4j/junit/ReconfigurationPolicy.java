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

package org.apache.logging.log4j.junit;

import org.apache.logging.log4j.core.LoggerContext;

/**
 * Indicates when to {@linkplain LoggerContext#reconfigure() reconfigure} the logging system during unit tests.
 *
 * @see LoggerContextSource
 * @since 2.14.0
 */
public enum ReconfigurationPolicy {
    /** Performs no reconfiguration of the logging system for the entire run of tests in a test class. This is the default. */
    NEVER,
    /** Performs a reconfiguration before executing each test. */
    BEFORE_EACH,
    /** Performs a reconfiguration after executing each test. */
    AFTER_EACH
}
