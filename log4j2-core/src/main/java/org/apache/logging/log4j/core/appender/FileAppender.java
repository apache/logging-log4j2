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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 *
 */
@Plugin(name="File",type="Core")
public class FileAppender extends OutputStreamAppender {

    public static final String FILE_NAME = "fileName";
    public static final String APPEND = "append";
    public static final String NAME = "name";
    public final String fileName;

    public FileAppender(String name, Layout layout, OutputStream os, String filename) {
        super(name, layout, os);
        this.fileName = filename;
    }

    @PluginFactory
    public static FileAppender createAppender(Node node) {
        Layout layout = createLayout(node);
        String fileName = null;
        String name = null;
        boolean isAppend = true;
        for (Map.Entry<String, String> attr : node.getAttributes().entrySet()) {
            if (attr.getKey().equalsIgnoreCase(FILE_NAME)) {
                fileName = attr.getValue();
            } else if (attr.getKey().equalsIgnoreCase(APPEND)) {
                isAppend = Boolean.parseBoolean(attr.getValue());
            } else if (attr.getKey().equalsIgnoreCase(NAME)) {
                name = attr.getValue();
            }
        }

        if (name == null) {
            logger.error("No name provided for Appender of type " + node.getName());
            return null;
        }

        if (fileName == null) {
            logger.error("No filename provided for Appender of type " + node.getName() +
                " with name " + name);
            return null;
        }

        try {
            OutputStream os = new FileOutputStream(fileName, isAppend);
            return new FileAppender(name, layout, os, fileName);
        } catch (FileNotFoundException ex) {
            logger.error("Unable to open file " + fileName, ex);
            return null;
        }
    }
}
