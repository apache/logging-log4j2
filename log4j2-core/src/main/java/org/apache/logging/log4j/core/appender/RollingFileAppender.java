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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.Filters;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
@Plugin(name="RollingFile",type="Core",elementType="appender",printObject=true)
public class RollingFileAppender extends OutputStreamAppender {

    public final String filePattern;
    private final RolloverStrategy[] strategies;
    private final Lock lock = new ReentrantLock();
    private final boolean bufferedIO;

    public RollingFileAppender(String name, Layout layout, RolloverStrategy[] strategies,
                               Filters filters, FileManager manager, String filePattern,
                               CompressionType type, boolean handleException, boolean immediateFlush,
                               boolean isBuffered) {
        super(name, layout, filters, handleException, immediateFlush, manager);
        this.filePattern = filePattern;
        this.strategies = strategies;
        manager.setCompressionType(type);
        this.bufferedIO = isBuffered;
    }

    /**
     * Write the log entry rolling over the file when required.

     * @param event The LogEvent.
     */
    @Override
    public void append(LogEvent event) {

        boolean rollover;
        lock.lock();
        try {
            for (RolloverStrategy strategy : strategies) {
                rollover = strategy.checkStrategy(event);
                if (rollover) {
                    performRollover();
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
        super.append(event);
    }

    private void performRollover() {
       // List<String> fileNames = getFileNames();
       // renameFiles(fileNames);
        String fileName = "";
        FileManager mgr = (FileManager) getManager();
        FileManager manager = FileManager.getFileManager(fileName, mgr.isAppend(), mgr.isLocking(), bufferedIO);
        manager.setCompressionType(mgr.getCompressionType());
        replaceManager(manager);
    }



    @PluginFactory
    public static RollingFileAppender createAppender(@PluginAttr("filePattern") String filePattern,
                                              @PluginAttr("append") String append,
                                              @PluginAttr("name") String name,
                                              @PluginAttr("compress") String compress,
                                              @PluginAttr("bufferedIO") String bufferedIO,
                                              @PluginAttr("immediateFlush") String immediateFlush,
                                              @PluginElement("strategies") RolloverStrategy[] strategies,
                                              @PluginElement("layout") Layout layout,
                                              @PluginElement("filters") Filters filters,
                                              @PluginAttr("suppressExceptions") String suppress) {

        boolean isAppend = append == null ? true : Boolean.valueOf(append);
        boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        boolean isBuffered = bufferedIO == null ? true : Boolean.valueOf(bufferedIO);;
        boolean isFlush = immediateFlush == null ? true : Boolean.valueOf(immediateFlush);;

        CompressionType type = CompressionType.NONE;

        if (name == null) {
            logger.error("No name provided for FileAppender");
            return null;
        }

        if (filePattern == null) {
            logger.error("No filename pattern provided for FileAppender with name "  + name);
            return null;
        }

        if (strategies == null) {
            logger.error("At least one RolloverStrategy must be provided");
            return null;
        }

        if (compress != null) {
            CompressionType t = CompressionType.valueOf(compress.toUpperCase());
            if (t != null) {
                type = t;
            }
        }

        String fileName = "";
        FileManager manager = FileManager.getFileManager(fileName, isAppend, false, isBuffered);
        if (manager == null) {
            return null;
        }

        return new RollingFileAppender(name, layout, strategies, filters, manager, filePattern, type, handleExceptions,
            isFlush, isBuffered);
    }
}
