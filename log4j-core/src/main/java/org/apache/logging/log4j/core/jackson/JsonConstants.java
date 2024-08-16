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
package org.apache.logging.log4j.core.jackson;

import aQute.bnd.annotation.baseline.BaselineIgnore;

/**
 * Keeps constants separate from any class that may depend on third party jars.
 */
@BaselineIgnore("2.24.0")
public final class JsonConstants {
    public static final String ELT_CAUSE = "cause";
    public static final String ELT_CONTEXT_MAP = "contextMap";
    public static final String ELT_CONTEXT_STACK = "contextStack";
    public static final String ELT_END_OF_BATCH = "endOfBatch";
    public static final String ELT_LEVEL = "level";
    public static final String ELT_LOGGER_FQCN = "loggerFqcn";
    public static final String ELT_LOGGER_NAME = "loggerName";
    public static final String ELT_MARKER = "marker";
    public static final String ELT_PARENTS = "parents";
    public static final String ELT_SOURCE = "source";
    public static final String ELT_SUPPRESSED = "suppressed";
    public static final String ELT_THREAD = "thread";
    public static final String ELT_THROWN = "thrown";
    public static final String ELT_MESSAGE = "message";
    public static final String ELT_EXTENDED_STACK_TRACE = "extendedStackTrace";
    public static final String ELT_NANO_TIME = "nanoTime";
    public static final String ELT_INSTANT = "instant";
    public static final String ELT_TIME_MILLIS = "timeMillis";

    private JsonConstants() {}
}
