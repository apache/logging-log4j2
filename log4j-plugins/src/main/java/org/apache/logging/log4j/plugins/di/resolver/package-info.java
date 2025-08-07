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
 * Factory resolvers are strategies for creating instances matching {@linkplain
 * org.apache.logging.log4j.plugins.di.spi.FactoryResolver#supportsKey(org.apache.logging.log4j.plugins.di.Key)
 * supported keys} from {@linkplain org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory existing instance
 * factories}. Resolvers should be
 * {@linkplain org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory#registerExtension(java.lang.Object)
 * registered} via a {@link org.apache.logging.log4j.plugins.di.spi.ConfigurableInstanceFactoryPostProcessor
 * ConfigurableInstanceFactoryPostProcessor} service.
 */
@Export
@Version("1.0.0")
@NullMarked
package org.apache.logging.log4j.plugins.di.resolver;

import org.jspecify.annotations.NullMarked;
import org.osgi.annotation.bundle.Export;
import org.osgi.annotation.versioning.Version;
