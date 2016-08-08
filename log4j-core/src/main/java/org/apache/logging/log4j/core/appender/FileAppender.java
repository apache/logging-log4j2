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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;

/**
 * File Appender.
 */
@Plugin(name = "File", category = "Core", elementType = "appender", printObject = true)
public final class FileAppender extends AbstractOutputStreamAppender<FileManager> {

    static final int DEFAULT_BUFFER_SIZE = 8192;
    private final String fileName;
    private final Advertiser advertiser;
    private Object advertisement;

    private FileAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
            final FileManager manager, final String filename, final boolean ignoreExceptions,
            final boolean immediateFlush, final Advertiser advertiser) {

        super(name, layout, filter, ignoreExceptions, immediateFlush, manager);
        if (advertiser != null) {
            final Map<String, String> configuration = new HashMap<>(layout.getContentFormat());
            configuration.putAll(manager.getContentFormat());
            configuration.put("contentType", layout.getContentType());
            configuration.put("name", name);
            advertisement = advertiser.advertise(configuration);
        }
        this.fileName = filename;
        this.advertiser = advertiser;
    }

    @Override
    public void stop() {
        super.stop();
        if (advertiser != null) {
            advertiser.unadvertise(advertisement);
        }
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
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param bufferedIo "true" if I/O should be buffered, "false" otherwise. The default is "true".
     * @param bufferSizeStr buffer size for buffered IO (default is 8192).
     * @param layout The layout to use to format the event. If no layout is provided the default PatternLayout
     * will be used.
     * @param filter The filter, if any, to use.
     * @param advertise "true" if the appender configuration should be advertised, "false" otherwise.
     * @param advertiseUri The advertised URI which can be used to retrieve the file contents.
     * @param config The Configuration
     * @return The FileAppender.
     * @deprecated Use {@link #createAppender(String, boolean, boolean, String, String, String, boolean, String, Layout<? extends Serializable>, Filter, String, String, boolean, Configuration)}
     */
    @Deprecated
    public static FileAppender createAppender(
            // @formatter:off
            final String fileName,
            final String append,
            final String locking,
            final String name,
            final String immediateFlush,
            final String ignore,
            final String bufferedIo,
            final String bufferSizeStr,
            Layout<? extends Serializable> layout,
            final Filter filter,
            final String advertise,
            final String advertiseUri,
            final Configuration config) {
        // @formatter:on
        final boolean isAppend = Booleans.parseBoolean(append, true);
        final boolean isLocking = Boolean.parseBoolean(locking);
        boolean isBuffered = Booleans.parseBoolean(bufferedIo, true);
        final boolean isAdvertise = Boolean.parseBoolean(advertise);
        if (isLocking && isBuffered) {
            if (bufferedIo != null) {
                LOGGER.warn("Locking and buffering are mutually exclusive. No buffering will occur for " + fileName);
            }
            isBuffered = false;
        }
        final int bufferSize = Integers.parseInt(bufferSizeStr, DEFAULT_BUFFER_SIZE);
        if (!isBuffered && bufferSize > 0) {
            LOGGER.warn("The bufferSize is set to {} but bufferedIO is not true: {}", bufferSize, bufferedIo);
        }
        final boolean isFlush = Booleans.parseBoolean(immediateFlush, true);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);

        if (name == null) {
            LOGGER.error("No name provided for FileAppender");
            return null;
        }

        if (fileName == null) {
            LOGGER.error("No filename provided for FileAppender with name "  + name);
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        final FileManager manager = FileManager.getFileManager(fileName, isAppend, isLocking, isBuffered, false,
                advertiseUri, layout, bufferSize, isFlush);
        if (manager == null) {
            return null;
        }

        return new FileAppender(name, layout, filter, manager, fileName, ignoreExceptions, !isBuffered || isFlush,
                isAdvertise ? config.getAdvertiser() : null);
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
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param bufferedIo "true" if I/O should be buffered, "false" otherwise. The default is "true".
     * @param bufferSize buffer size for buffered IO (default is 8192).
     * @param layout The layout to use to format the event. If no layout is provided the default PatternLayout
     * will be used.
     * @param filter The filter, if any, to use.
     * @param advertise "true" if the appender configuration should be advertised, "false" otherwise.
     * @param advertiseUri The advertised URI which can be used to retrieve the file contents.
     * @param lazyCreate true if you want to lazy-create the file (a.k.a. on-demand.)
     * @param config The Configuration
     * @return The FileAppender.
     * @since 2.7
     */
    @PluginFactory
    public static FileAppender createAppender(
            // @formatter:off
            @PluginAttribute("fileName") final String fileName,
            @PluginAttribute(value = "append", defaultBoolean = true) final boolean append,
            @PluginAttribute("locking") final boolean locking,
            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "immediateFlush", defaultBoolean = true) final boolean immediateFlush,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions,
            @PluginAttribute(value = "bufferedIo", defaultBoolean = true) boolean bufferedIo,
            @PluginAttribute(value = "bufferSize", defaultInt = DEFAULT_BUFFER_SIZE) final int bufferSize,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("advertise") final boolean advertise,
            @PluginAttribute("advertiseUri") final String advertiseUri,
            @PluginAttribute("lazyCreate") final boolean lazyCreate,
            @PluginConfiguration final Configuration config) {
             // @formatter:on
        if (locking && bufferedIo) {
            LOGGER.warn("Locking and buffering are mutually exclusive. No buffering will occur for {}", fileName);
            bufferedIo = false;
        }
        if (!bufferedIo && bufferSize > 0) {
            LOGGER.warn("The bufferSize is set to {} but bufferedIo is not true: {}", bufferSize, bufferedIo);
        }
        if (name == null) {
            LOGGER.error("No name provided for FileAppender");
            return null;
        }
        if (fileName == null) {
            LOGGER.error("No filename provided for FileAppender with name {}", name);
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        final FileManager manager = FileManager.getFileManager(fileName, append, locking, bufferedIo, lazyCreate,
                advertiseUri, layout, bufferSize, immediateFlush);
        if (manager == null) {
            return null;
        }

        return new FileAppender(name, layout, filter, manager, fileName, ignoreExceptions, !bufferedIo || immediateFlush,
                advertise ? config.getAdvertiser() : null);
    }
}
