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
 * Utilities for formatting log event {@link org.apache.logging.log4j.core.time.Instant}s.
 * <h2>Internal usage only!</h2>
 * <p>
 * This package is intended only for internal Log4j usage.
 * <b>Log4j users should not use this package!</b>
 * This package is not subject to any backward compatibility concerns.
 * </p>
 *
 * @since 2.25.0
 */
@Export
@ExportTo("org.apache.logging.log4j.layout.template.json")
@Version("2.25.0")
@NullMarked
package org.apache.logging.log4j.core.util.internal.instant;

import aQute.bnd.annotation.jpms.ExportTo;
import org.jspecify.annotations.NullMarked;
import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.versioning.Version;
