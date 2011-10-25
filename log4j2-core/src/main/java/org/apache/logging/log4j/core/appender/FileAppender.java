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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 *
 */
@Plugin(name="File",type="Core",elementType="appender",printObject=true)
public class FileAppender extends OutputStreamAppender {

    public final String fileName;

    public FileAppender(String name, Layout layout, Filter filter, FileManager manager, String filename,
                        boolean handleException, boolean immediateFlush) {
        super(name, layout, filter, handleException, immediateFlush, manager);
        this.fileName = filename;
    }

    @PluginFactory
    public static FileAppender createAppender(@PluginAttr("fileName") String fileName,
                                              @PluginAttr("append") String append,
                                              @PluginAttr("locking") String locking,
                                              @PluginAttr("name") String name,
                                              @PluginAttr("immediateFlush") String immediateFlush,
                                              @PluginAttr("suppressExceptions") String suppress,
                                              @PluginAttr("bufferedIO") String bufferedIO,
                                              @PluginElement("layout") Layout layout,
                                              @PluginElement("filters") Filter filter) {

        boolean isAppend = append == null ? true : Boolean.valueOf(append);
        boolean isLocking = locking == null ? false : Boolean.valueOf(locking);
        boolean isBuffered = bufferedIO == null ? true : Boolean.valueOf(bufferedIO);;
        if (isLocking && isBuffered) {
            if (bufferedIO != null) {
                logger.warn("Locking and buffering are mutually exclusive. No buffereing will occur for " + fileName);
            }
            isBuffered = false;
        }
        boolean isFlush = immediateFlush == null ? true : Boolean.valueOf(immediateFlush);;
        boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);

        if (name == null) {
            logger.error("No name provided for FileAppender");
            return null;
        }

        if (fileName == null) {
            logger.error("No filename provided for FileAppender with name "  + name);
            return null;
        }

        FileManager manager = FileManager.getFileManager(fileName, isAppend, isLocking, isBuffered);
        if (manager == null) {
            return null;
        }
        return new FileAppender(name, layout, filter, manager, fileName, handleExceptions, isFlush);
    }
}
