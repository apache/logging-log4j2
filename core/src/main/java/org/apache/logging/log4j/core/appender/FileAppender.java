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
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * File Appender.
 */
@Plugin(name = "File", type = "Core", elementType = "appender", printObject = true)
public final class FileAppender extends AbstractOutputStreamAppender {

    private final String fileName;

    private FileAppender(final String name, final Layout layout, final Filter filter, final FileManager manager,
                         final String filename, final boolean handleException, final boolean immediateFlush) {
        super(name, layout, filter, handleException, immediateFlush, manager);
        this.fileName = filename;
    }

    /**
     * Returns the file name this appender is associated with.
     * @return The File name.
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Create a File Appender.
     * @param fileName The name and path of the file.
     * @param append "True" if the file should be appended to, "false" if it should be overwritten.
     * The default is "true".
     * @param locking "True" if the file should be locked. The default is "false".
     * @param name The name of the Appender.
     * @param immediateFlush "true" if the contents should be flushed on every write, "false" otherwise. The default
     * is "true".
     * @param suppress "true" if exceptions should be hidden from the application, "false" otherwise.
     * The default is "true".
     * @param bufferedIO "true" if I/O should be buffered, "false" otherwise. The default is "true".
     * @param layout The layout to use to format the event. If no layout is provided the default PatternLayout
     * will be used.
     * @param filter The filter, if any, to use.
     * @return The FileAppender.
     */
    @PluginFactory
    public static FileAppender createAppender(@PluginAttr("fileName") final String fileName,
                                              @PluginAttr("append") final String append,
                                              @PluginAttr("locking") final String locking,
                                              @PluginAttr("name") final String name,
                                              @PluginAttr("immediateFlush") final String immediateFlush,
                                              @PluginAttr("suppressExceptions") final String suppress,
                                              @PluginAttr("bufferedIO") final String bufferedIO,
                                              @PluginElement("layout") Layout layout,
                                              @PluginElement("filters") final Filter filter) {

        final boolean isAppend = append == null ? true : Boolean.valueOf(append);
        final boolean isLocking = locking == null ? false : Boolean.valueOf(locking);
        boolean isBuffered = bufferedIO == null ? true : Boolean.valueOf(bufferedIO);
        if (isLocking && isBuffered) {
            if (bufferedIO != null) {
                LOGGER.warn("Locking and buffering are mutually exclusive. No buffereing will occur for " + fileName);
            }
            isBuffered = false;
        }
        final boolean isFlush = immediateFlush == null ? true : Boolean.valueOf(immediateFlush);
        final boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);

        if (name == null) {
            LOGGER.error("No name provided for FileAppender");
            return null;
        }

        if (fileName == null) {
            LOGGER.error("No filename provided for FileAppender with name "  + name);
            return null;
        }

        final FileManager manager = FileManager.getFileManager(fileName, isAppend, isLocking, isBuffered);
        if (manager == null) {
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createLayout(null, null, null, null);
        }
        return new FileAppender(name, layout, filter, manager, fileName, handleExceptions, isFlush);
    }
}
