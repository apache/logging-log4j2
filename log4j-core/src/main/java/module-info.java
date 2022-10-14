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
import org.apache.logging.log4j.core.impl.DefaultCallback;
import org.apache.logging.log4j.core.impl.Log4jProvider;
import org.apache.logging.log4j.core.impl.ThreadContextDataProvider;
import org.apache.logging.log4j.core.message.ExtendedThreadInfoFactory;
import org.apache.logging.log4j.core.plugins.Log4jPlugins;
import org.apache.logging.log4j.core.script.ScriptManagerFactory;
import org.apache.logging.log4j.core.util.ContextDataProvider;
import org.apache.logging.log4j.core.util.WatchEventService;
import org.apache.logging.log4j.message.ThreadDumpMessage.ThreadInfoFactory;
import org.apache.logging.log4j.plugins.di.InjectorCallback;
import org.apache.logging.log4j.plugins.model.PluginService;
import org.apache.logging.log4j.spi.Provider;

module org.apache.logging.log4j.core {
    exports org.apache.logging.log4j.core;
    exports org.apache.logging.log4j.core.appender;
    exports org.apache.logging.log4j.core.appender.db;
    exports org.apache.logging.log4j.core.appender.nosql;
    exports org.apache.logging.log4j.core.appender.rewrite;
    exports org.apache.logging.log4j.core.appender.rolling;
    exports org.apache.logging.log4j.core.appender.rolling.action;
    exports org.apache.logging.log4j.core.appender.routing;
    exports org.apache.logging.log4j.core.async;
    exports org.apache.logging.log4j.core.config;
    exports org.apache.logging.log4j.core.config.arbiters;
    exports org.apache.logging.log4j.core.config.builder.api;
    exports org.apache.logging.log4j.core.config.builder.impl;
    exports org.apache.logging.log4j.core.config.composite;
    exports org.apache.logging.log4j.core.config.json;
    exports org.apache.logging.log4j.core.config.plugins;
    exports org.apache.logging.log4j.core.config.plugins.convert;
    exports org.apache.logging.log4j.core.config.plugins.util;
    exports org.apache.logging.log4j.core.config.plugins.visit;
    exports org.apache.logging.log4j.core.config.properties;
    exports org.apache.logging.log4j.core.config.status;
    exports org.apache.logging.log4j.core.config.xml;
    exports org.apache.logging.log4j.core.config.yaml;
    exports org.apache.logging.log4j.core.filter;
    exports org.apache.logging.log4j.core.impl;
    exports org.apache.logging.log4j.core.jmx;
    exports org.apache.logging.log4j.core.layout;
    exports org.apache.logging.log4j.core.lookup;
    exports org.apache.logging.log4j.core.message;
    exports org.apache.logging.log4j.core.net;
    exports org.apache.logging.log4j.core.net.ssl;
    exports org.apache.logging.log4j.core.osgi;
    exports org.apache.logging.log4j.core.parser;
    exports org.apache.logging.log4j.core.pattern;
    exports org.apache.logging.log4j.core.script;
    exports org.apache.logging.log4j.core.selector;
    exports org.apache.logging.log4j.core.time;
    exports org.apache.logging.log4j.core.tools;
    exports org.apache.logging.log4j.core.tools.picocli;
    exports org.apache.logging.log4j.core.util;
    exports org.apache.logging.log4j.core.util.datetime;

    // Required Dependencies
    requires transitive org.apache.logging.log4j;
    requires transitive org.apache.logging.log4j.plugins;
    // Optional Dependencies
    requires static java.desktop;
    requires static java.management;
    requires static java.sql;
    requires static java.rmi;
    requires static java.xml;
    requires static com.lmax.disruptor;
    requires static org.jctools.core;
    requires static org.osgi.framework;
    requires static com.conversantmedia.disruptor;
    requires static com.fasterxml.jackson.core;
    requires static com.fasterxml.jackson.databind;
    requires static com.fasterxml.jackson.dataformat.xml;
    requires static com.fasterxml.jackson.dataformat.yaml;
    requires static org.apache.commons.compress;
    requires static org.fusesource.jansi;

    // sun.misc.Unsafe::invokeCleaner via MemoryMappedFileManager
    requires static jdk.unsupported;

    uses ContextDataProvider;
    uses WatchEventService;
    uses ScriptManagerFactory;
    provides ThreadInfoFactory with ExtendedThreadInfoFactory;
    provides ContextDataProvider with ThreadContextDataProvider;
    provides Provider with Log4jProvider;
    provides PluginService with Log4jPlugins;
    provides InjectorCallback with DefaultCallback;
}
