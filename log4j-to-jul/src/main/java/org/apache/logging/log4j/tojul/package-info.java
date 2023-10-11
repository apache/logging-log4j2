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
 * Java JDK java.util.logging (JUL) bridge.
 * This sends all Log4j logs to JUL (not the other way around, there is another module for the opposite direction).
 *
 * @author <a href="http://www.vorburger.ch">Michael Vorburger.ch</a> for Google
 */
@Export
@Version("2.20.1")
package org.apache.logging.log4j.tojul;

import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.versioning.Version;
