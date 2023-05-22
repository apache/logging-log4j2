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

import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.plugins.di.resolver.GenericFactoryResolversPostProcessor;
import org.apache.logging.log4j.plugins.di.resolver.PluginAnnotationFactoryResolversPostProcessor;
import org.apache.logging.log4j.plugins.di.spi.ConfigurableInstanceFactoryPostProcessor;
import org.apache.logging.log4j.plugins.di.spi.SingletonScopePostProcessor;
import org.apache.logging.log4j.plugins.model.PluginService;

/**
 * Log4j plugin annotations and dependency injection system. Plugins encompass a variety of customizable
 * Log4j interfaces and classes that are addressable by {@linkplain org.apache.logging.log4j.plugins.Named name} or type
 * including {@linkplain org.apache.logging.log4j.plugins.Configurable configurable plugins} which are created from a
 * parsed tree of {@linkplain org.apache.logging.log4j.plugins.Node configuration nodes} along with other
 * {@linkplain org.apache.logging.log4j.plugins.Namespace namespaces} for different dependency injection purposes.
 *
 * @see Inject
 * @see Plugin
 * @see PluginFactory
 * @see InstanceFactory
 * @see ConfigurableInstanceFactory
 * @see ConfigurableInstanceFactoryPostProcessor
 */
module org.apache.logging.log4j.plugins {
    exports org.apache.logging.log4j.plugins;
    exports org.apache.logging.log4j.plugins.condition;
    exports org.apache.logging.log4j.plugins.convert;
    exports org.apache.logging.log4j.plugins.di;
    exports org.apache.logging.log4j.plugins.di.resolver;
    exports org.apache.logging.log4j.plugins.di.spi;
    exports org.apache.logging.log4j.plugins.model;
    exports org.apache.logging.log4j.plugins.name;
    exports org.apache.logging.log4j.plugins.util;
    exports org.apache.logging.log4j.plugins.validation;
    exports org.apache.logging.log4j.plugins.validation.constraints;
    exports org.apache.logging.log4j.plugins.validation.validators;

    requires org.apache.logging.log4j;
    requires static org.osgi.framework;

    // import generated plugin metadata
    uses PluginService;
    // extensions to ConfigurableInstanceFactory
    uses ConfigurableInstanceFactoryPostProcessor;

    provides ConfigurableInstanceFactoryPostProcessor with
            SingletonScopePostProcessor,
            PluginAnnotationFactoryResolversPostProcessor,
            GenericFactoryResolversPostProcessor;
}
