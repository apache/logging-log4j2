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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.Filters;

import java.io.OutputStream;

/**
 * ConsoleAppender appends log events to <code>System.out</code> or
 * <code>System.err</code> using a layout specified by the user. The
 * default target is <code>System.out</code>.
 * @doubt accessing System.out or .err as a byte stream instead of a writer
 *    bypasses the JVM's knowledge of the proper encoding. (RG) Encoding
 * is handled within the Layout. Typically, a Layout will generate a String
 * and then call getBytes which may use a configured encoding or the system
 * default. OTOH, a Writer cannot print byte streams.
 */
@Plugin(name="Console",type="Core",elementType="appender",printObject=true)
public class ConsoleAppender extends OutputStreamAppender {

    public static final String LAYOUT = "layout";
    public static final String TARGET = "target";
    public static final String NAME = "name";

    private static ManagerFactory factory = new ConsoleManagerFactory();

    public enum Target {
        SYSTEM_OUT, SYSTEM_ERR
    }

    public ConsoleAppender(String name, Layout layout) {
        this(name, layout, null, getManager(Target.SYSTEM_OUT));

    }

    public ConsoleAppender(String name, Layout layout, Filters filters, OutputStreamManager manager) {
        super(name, layout, filters, true, true, manager);
    }

    @PluginFactory
    public static ConsoleAppender createAppender(@PluginElement("layout") Layout layout,
                                                 @PluginElement("filters") Filters filters,
                                                 @PluginAttr("target") String t,
                                                 @PluginAttr("name") String name) {
        if (name == null) {
            logger.error("No name provided for ConsoleAppender");
            return null;
        }
        Target target = t == null ? Target.SYSTEM_OUT : Target.valueOf(t);
        return new ConsoleAppender(name, layout, filters, getManager(target));
    }

    private static OutputStreamManager getManager(Target target) {
        String type = target.name();
        OutputStream os = target == Target.SYSTEM_OUT ? System.out : System.err;
        OutputStreamManager manager = OutputStreamManager.getManager(target.name(), factory,
            new FactoryData(os, type));
        return manager;
    }

    private static class FactoryData {
        OutputStream os;
        String type;

        public FactoryData(OutputStream os, String type) {
            this.os = os;
            this.type = type;
        }
    }

    private static class ConsoleManagerFactory implements ManagerFactory<OutputStreamManager, FactoryData> {

        public OutputStreamManager createManager(String name, FactoryData data) {
            return new OutputStreamManager(data.os, data.type);
        }
    }

}
