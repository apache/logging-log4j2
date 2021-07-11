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
module org.apache.logging.log4j.plugins {
    exports org.apache.logging.log4j.plugins;
    exports org.apache.logging.log4j.plugins.convert;
    exports org.apache.logging.log4j.plugins.di;
    exports org.apache.logging.log4j.plugins.di.spi;
    exports org.apache.logging.log4j.plugins.processor;
    exports org.apache.logging.log4j.plugins.util;
    exports org.apache.logging.log4j.plugins.validation;
    exports org.apache.logging.log4j.plugins.validation.constraints;
    exports org.apache.logging.log4j.plugins.validation.validators;
    exports org.apache.logging.log4j.plugins.bind;
    exports org.apache.logging.log4j.plugins.inject;
    exports org.apache.logging.log4j.plugins.name;

    requires transitive java.compiler; // TODO: break out annotation processor into separate module
    requires transitive org.apache.logging.log4j;
    requires transitive org.osgi.framework;

    provides org.apache.logging.log4j.plugins.processor.PluginService with org.apache.logging.log4j.plugins.convert.plugins.Log4jPlugins;
    provides org.apache.logging.log4j.plugins.di.spi.BeanInfoService with org.apache.logging.log4j.plugins.convert.plugins.Log4jBeanInfo;
    provides javax.annotation.processing.Processor with org.apache.logging.log4j.plugins.processor.PluginProcessor, org.apache.logging.log4j.plugins.processor.BeanProcessor;

    uses org.apache.logging.log4j.plugins.processor.PluginService;
//    uses org.apache.logging.log4j.plugins.di.spi.BeanInfoService;
}
