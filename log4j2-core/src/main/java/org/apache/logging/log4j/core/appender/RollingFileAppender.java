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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * An appender that writes to files andd can roll over at intervals.
 */
@Plugin(name="RollingFile",type="Core",elementType="appender",printObject=true)
public class RollingFileAppender extends OutputStreamAppender {

    public final String fileName;
    public final String filePattern;
    private final TriggeringPolicy policy;
    private final RolloverStrategy strategy;

    private RollingFileAppender(String name, Layout layout, TriggeringPolicy policy, RolloverStrategy strategy,
                               Filter filter, RollingFileManager manager, String fileName, String filePattern,
                               boolean handleException, boolean immediateFlush) {
        super(name, layout, filter, handleException, immediateFlush, manager);
        this.fileName = fileName;
        this.filePattern = filePattern;
        this.policy = policy;
        this.strategy = strategy;
        policy.initialize(manager);
    }

    /**
     * Write the log entry rolling over the file when required.

     * @param event The LogEvent.
     */
    @Override
    public void append(LogEvent event) {
        ((RollingFileManager) getManager()).checkRollover(event, policy, strategy);
        super.append(event);
    }

    /**
     * Create a RollingFileAppender.
     * @param fileName The name of the file that is actively written to. (required).
     * @param filePattern The pattern of the file name to use on rollover. (required).
     * @param append If true, events are appended to the file. If false, the file
     * is overwritten when opened. Defaults to "true"
     * @param name The name of the Appender (required).
     * @param bufferedIO When true, I/O will be buffered. Defaults to "true".
     * @param immediateFlush When true, events are immediately flushed. Defaults to "true".
     * @param policy The triggering policy. (required).
     * @param strategy The rollover strategy. Defaults to DefaultRolloverStrategy.
     * @param layout The layout to use (defaults to the default PatternLayout).
     * @param filter The Filter or null.
     * @param suppress "true" if exceptions should be hidden from the application, "false" otherwise.
     * The default is "true".
     * @return
     */
    @PluginFactory
    public static RollingFileAppender createAppender(@PluginAttr("fileName") String fileName,
                                              @PluginAttr("filePattern") String filePattern,
                                              @PluginAttr("append") String append,
                                              @PluginAttr("name") String name,
                                              @PluginAttr("bufferedIO") String bufferedIO,
                                              @PluginAttr("immediateFlush") String immediateFlush,
                                              @PluginElement("policy") TriggeringPolicy policy,
                                              @PluginElement("strategy") RolloverStrategy strategy,
                                              @PluginElement("layout") Layout layout,
                                              @PluginElement("filter") Filter filter,
                                              @PluginAttr("suppressExceptions") String suppress) {

        boolean isAppend = append == null ? true : Boolean.valueOf(append);
        boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        boolean isBuffered = bufferedIO == null ? true : Boolean.valueOf(bufferedIO);;
        boolean isFlush = immediateFlush == null ? true : Boolean.valueOf(immediateFlush);;

        if (name == null) {
            logger.error("No name provided for FileAppender");
            return null;
        }

        if (fileName == null) {
            logger.error("No filename was provided for FileAppender with name "  + name);
            return null;
        }

        if (filePattern == null) {
            logger.error("No filename pattern provided for FileAppender with name "  + name);
            return null;
        }

        if (policy == null) {
            logger.error("A TriggeringPolicy must be provided");
            return null;
        }

        if (strategy == null) {
            strategy = DefaultRolloverStrategy.createStrategy(null, null);
        }

        RollingFileManager manager = RollingFileManager.getFileManager(fileName, filePattern, isAppend, isBuffered);
        if (manager == null) {
            return null;
        }

        if (layout == null) {
            layout = PatternLayout.createLayout(null, null, null, null);
        }

        return new RollingFileAppender(name, layout, policy, strategy, filter, manager, fileName, filePattern,
            handleExceptions, isFlush);
    }
}
