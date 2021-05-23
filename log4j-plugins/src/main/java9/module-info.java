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
    exports org.apache.logging.log4j.plugins.processor;
    exports org.apache.logging.log4j.plugins.util;
    exports org.apache.logging.log4j.plugins.validation;
    exports org.apache.logging.log4j.plugins.validation.constraints;
    exports org.apache.logging.log4j.plugins.validation.validators;
    exports org.apache.logging.log4j.plugins.bind;
    exports org.apache.logging.log4j.plugins.inject;
    exports org.apache.logging.log4j.plugins.name;

    requires java.compiler;
    requires org.apache.logging.log4j;
    requires transitive org.osgi.framework;

    provides org.apache.logging.log4j.plugins.processor.PluginService with org.apache.logging.log4j.plugins.convert.plugins.Log4jPlugins;
    provides javax.annotation.processing.Processor with org.apache.logging.log4j.plugins.processor.PluginProcessor;

    uses org.apache.logging.log4j.plugins.processor.PluginService;
}
