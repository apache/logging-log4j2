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
 * Java annotation processor for pre-scanning Log4j 2 plugins. This is provided as an alternative to using the
 * executable {@link org.apache.logging.log4j.core.config.plugins.util.PluginManager} class in your build process.
 */
@Export
@Version("2.25.0")
package org.apache.logging.log4j.core.config.plugins.processor;

import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.versioning.Version;
