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
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.Map;

/**
 * ConsoleAppender appends log events to <code>System.out</code> or
 * <code>System.err</code> using a layout specified by the user. The
 * default target is <code>System.out</code>.
 * @doubt accessing System.out or .err as a byte stream instead of a writer
 *    bypasses the JVM's knowledge of the proper encoding.
 */
@Plugin(name="Console",type="Core")
public class ConsoleAppender extends OutputStreamAppender {

    public static final String LAYOUT = "layout";
    public static final String TARGET = "target";
    public static final String NAME = "name";

    public enum Target {
        SYSTEM_OUT, SYSTEM_ERR
    }

    public ConsoleAppender(String name, Layout layout) {
        this(name, layout, Target.SYSTEM_OUT);

    }

    public ConsoleAppender(String name, Layout layout, Target target) {
        super(name, layout, target == Target.SYSTEM_OUT ? System.out : System.err);
    }

    @PluginFactory
    public static ConsoleAppender createAppender(Node node) {
        Layout layout = createLayout(node);
        String t = null;
        String name = null;
        for (Map.Entry<String, String> attr : node.getAttributes().entrySet()) {
            if (attr.getKey().equalsIgnoreCase(TARGET)) {
                t = attr.getValue().toUpperCase();
            } else if (attr.getKey().equalsIgnoreCase(NAME)) {
                name = attr.getValue();
            }
        }
        if (name == null) {
            logger.error("No name provided for Appender of type " + node.getName());
            return null;
        }
        Target target = t == null ? Target.SYSTEM_OUT : Target.valueOf(t);
        return new ConsoleAppender(name, layout, target);
    }

}
