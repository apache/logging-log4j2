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
 * Log4j 2 network support. This package (and subpackages) include support for:
 * <ul>
 *     <li>Java Message System appenders (both queue-based and topic-based)</li>
 *     <li>Zeroconf support for finding logging systems</li>
 *     <li>TCP and UDP socket servers for sending and receiving log events</li>
 *     <li>JavaMail appenders for sending log events over SMTP</li>
 *     <li>Syslog network log event support</li>
 * </ul>
 */
@Export
@Version("2.20.2")
package org.apache.logging.log4j.core.net;

import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.versioning.Version;
