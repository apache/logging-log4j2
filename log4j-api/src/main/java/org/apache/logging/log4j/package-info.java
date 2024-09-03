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
/**
 * Public API for Log4j 2.
 *
 * <p>
 *     The main entry point into Log4j is the {@link org.apache.logging.log4j.LogManager} class which can be used to
 *     obtain {@link org.apache.logging.log4j.Logger} objects, the current
 *     {@link org.apache.logging.log4j.spi.LoggerContext}, a new {@code LoggerContext}, and the current
 *     {@link org.apache.logging.log4j.spi.LoggerContextFactory}.
 * </p>
 * <p>
 *     {@link org.apache.logging.log4j.Marker} objects can be obtained through the
 *     {@link org.apache.logging.log4j.MarkerManager}. The MDC (Mapped Diagnostic Context) can be
 *     used through the {@link org.apache.logging.log4j.ThreadContext} class.
 * </p>
 *
 * @see <a href="https://logging.apache.org/log4j/2.x/manual/api.html">Log4j 2 API manual</a>
 */
@Export
@Version("2.20.2")
package org.apache.logging.log4j;

import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.versioning.Version;
