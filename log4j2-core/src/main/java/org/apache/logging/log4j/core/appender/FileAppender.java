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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 *
 */
@Plugin(name="File",type="Core",elementType="appender")
public class FileAppender extends OutputStreamAppender {

    public static final String FILE_NAME = "fileName";
    public static final String APPEND = "append";
    public final String fileName;

    public FileAppender(String name, Layout layout, Filter[] filters, OutputStream os, String filename) {
        super(name, layout, filters, os);
        this.fileName = filename;
    }

    @PluginFactory
    public static FileAppender createAppender(@PluginAttr("fileName") String fileName,
                                              @PluginAttr("append") String append,
                                              @PluginAttr("name") String name,
                                              @PluginElement("layout") Layout layout,
                                              @PluginElement("filters") Filter[] filters) {

        boolean isAppend = append == null ? true : Boolean.valueOf(append);

        if (name == null) {
            logger.error("No name provided for FileAppender");
            return null;
        }

        if (fileName == null) {
            logger.error("No filename provided for FileAppender with name "  + name);
            return null;
        }

        try {
            OutputStream os = new FileOutputStream(fileName, isAppend);
            return new FileAppender(name, layout, filters, os, fileName);
        } catch (FileNotFoundException ex) {
            logger.error("Unable to open file " + fileName, ex);
            return null;
        }
    }
}
