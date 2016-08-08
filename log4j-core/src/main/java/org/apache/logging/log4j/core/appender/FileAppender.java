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
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;

/**
 * File Appender.
 */
@Plugin(name = "File", category = "Core", elementType = "appender", printObject = true)
public final class FileAppender extends AbstractOutputStreamAppender<FileManager> {

    /**
     * Builds FileAppender instances.
     */
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<FileAppender> {

        @PluginBuilderAttribute
        @Required
        private String fileName;

        @PluginBuilderAttribute
        private boolean append = true;

        @PluginBuilderAttribute
        private boolean locking;

        @PluginBuilderAttribute
        @Required
        private String name;

        @PluginBuilderAttribute
        private boolean immediateFlush = true;

        @PluginBuilderAttribute
        private boolean ignoreExceptions = true;

        @PluginBuilderAttribute
        private boolean bufferedIo = true;

        @PluginBuilderAttribute
        private int bufferSize = DEFAULT_BUFFER_SIZE;

        @PluginElement("Layout")
        private Layout<? extends Serializable> layout;

        @PluginElement("Filter")
        private Filter filter;

        @PluginBuilderAttribute
        private boolean advertise;

        @PluginBuilderAttribute
        private String advertiseUri;

        @PluginBuilderAttribute
        private boolean lazyCreate;

        @PluginConfiguration
        private Configuration config;

        @Override
        public FileAppender build() {
            if (locking && bufferedIo) {
                LOGGER.warn("Locking and buffering are mutually exclusive. No buffering will occur for {}", fileName);
                bufferedIo = false;
            }
            if (!bufferedIo && bufferSize > 0) {
                LOGGER.warn("The bufferSize is set to {} but bufferedIo is not true: {}", bufferSize, bufferedIo);
            }
            if (layout == null) {
                layout = PatternLayout.createDefaultLayout();
            }

            final FileManager manager = FileManager.getFileManager(fileName, append, locking, bufferedIo, lazyCreate,
                    advertiseUri, layout, bufferSize, immediateFlush);
            if (manager == null) {
                return null;
            }

            return new FileAppender(name, layout, filter, manager, fileName, ignoreExceptions,
                    !bufferedIo || immediateFlush, advertise ? config.getAdvertiser() : null);
        }

        public String getAdvertiseUri() {
            return advertiseUri;
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public Configuration getConfig() {
            return config;
        }

        public String getFileName() {
            return fileName;
        }

        public Filter getFilter() {
            return filter;
        }

        public Layout<? extends Serializable> getLayout() {
            return layout;
        }

        public String getName() {
            return name;
        }

        public boolean isAdvertise() {
            return advertise;
        }

        public boolean isAppend() {
            return append;
        }

        public boolean isBufferedIo() {
            return bufferedIo;
        }

        public boolean isIgnoreExceptions() {
            return ignoreExceptions;
        }

        public boolean isImmediateFlush() {
            return immediateFlush;
        }

        public boolean isLazyCreate() {
            return lazyCreate;
        }

        public boolean isLocking() {
            return locking;
        }

        public Builder withAdvertise(final boolean advertise) {
            this.advertise = advertise;
            return this;
        }

        public Builder withAdvertiseUri(final String advertiseUri) {
            this.advertiseUri = advertiseUri;
            return this;
        }

        public Builder withAppend(final boolean append) {
            this.append = append;
            return this;
        }

        public Builder withBufferedIo(final boolean bufferedIo) {
            this.bufferedIo = bufferedIo;
            return this;
        }

        public Builder withBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder withConfig(final Configuration config) {
            this.config = config;
            return this;
        }

        public Builder withFileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder withFilter(final Filter filter) {
            this.filter = filter;
            return this;
        }

        public Builder withIgnoreExceptions(final boolean ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
            return this;
        }

        public Builder withImmediateFlush(final boolean immediateFlush) {
            this.immediateFlush = immediateFlush;
            return this;
        }

        public Builder withLayout(final Layout<? extends Serializable> layout) {
            this.layout = layout;
            return this;
        }

        public Builder withLazyCreate(final boolean lazyCreate) {
            this.lazyCreate = lazyCreate;
            return this;
        }

        public Builder withLocking(final boolean locking) {
            this.locking = locking;
            return this;
        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }
    }
    
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
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
     * @param bufferSizeStr buffer size for buffered IO (default is 8192).
     * @param layout The layout to use to format the event. If no layout is provided the default PatternLayout
     * will be used.
     * @param filter The filter, if any, to use.
     * @param advertise "true" if the appender configuration should be advertised, "false" otherwise.
     * @param advertiseUri The advertised URI which can be used to retrieve the file contents.
     * @param config The Configuration
     * @return The FileAppender.
     * @deprecated Use {@link #newBuilder()}
     */
    @Deprecated
    public static FileAppender createAppender(
            // @formatter:off
            final String fileName,
            final String append,
            final String locking,
            final String name,
            final String immediateFlush,
            final String ignoreExceptions,
            final String bufferedIo,
            final String bufferSizeStr,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final String advertise,
            final String advertiseUri,
            final Configuration config) {
        return newBuilder()
            .withAdvertise(Boolean.parseBoolean(advertise))
            .withAdvertiseUri(advertiseUri)
            .withAppend(Booleans.parseBoolean(append, true))
            .withBufferedIo(Booleans.parseBoolean(bufferedIo, true))
            .withBufferSize(Integers.parseInt(bufferSizeStr, DEFAULT_BUFFER_SIZE))
            .withConfig(config)
            .withFileName(fileName)
            .withFilter(filter)
            .withIgnoreExceptions(Booleans.parseBoolean(ignoreExceptions, true))
            .withImmediateFlush(Booleans.parseBoolean(immediateFlush, true))
            .withLayout(layout)
            .withLocking(Boolean.parseBoolean(locking))
            .withName(name)
            .build();
        // @formatter:on
    }
    
    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }
    
    private final String fileName;

    private final Advertiser advertiser;

    private final Object advertisement;

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
        } else {
            advertisement = null;
        }
        this.fileName = filename;
        this.advertiser = advertiser;
    }

    /**
     * Returns the file name this appender is associated with.
     * @return The File name.
     */
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public void stop() {
        super.stop();
        if (advertiser != null) {
            advertiser.unadvertise(advertisement);
        }
    }
}
