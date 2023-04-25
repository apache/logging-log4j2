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

/**
 * Keeps constants separate from any class that may depend on third party jars.
 */
public final class XmlConstants {
    public static final String ELT_CAUSE = "Cause";
    public static final String ELT_CONTEXT_MAP = "ContextMap";
    public static final String ELT_CONTEXT_STACK = "ContextStack";
    public static final String ELT_CONTEXT_STACK_ITEM = "ContextStackItem";
    public static final String ELT_EVENT = "Event";
    public static final String ELT_EXTENDED_STACK_TRACE = "ExtendedStackTrace";
    public static final String ELT_EXTENDED_STACK_TRACE_ITEM = "ExtendedStackTraceItem";
    public static final String ELT_TIME_MILLIS = "TimeMillis";
    public static final String ELT_INSTANT = "Instant";
    public static final String ELT_MARKER = "Marker";
    public static final String ELT_MESSAGE = "Message";
    public static final String ELT_PARENTS = "Parents";
    public static final String ELT_SOURCE = "Source";
    public static final String ELT_SUPPRESSED = "Suppressed";
    public static final String ELT_SUPPRESSED_ITEM = "SuppressedItem";
    public static final String ELT_THROWN = "Thrown";
    public static final String XML_NAMESPACE = "http://logging.apache.org/log4j/2.0/events";
}
