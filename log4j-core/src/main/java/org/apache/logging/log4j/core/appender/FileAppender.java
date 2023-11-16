/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;

/**
 * File Appender.
 */
@Plugin(
        name = FileAppender.PLUGIN_NAME,
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE,
        printObject = true)
public final class FileAppender extends AbstractOutputStreamAppender<FileManager> {

    public static final String PLUGIN_NAME = "File";

    /**
     * Builds FileAppender instances.
     *
     * @param <B>
     *            The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<FileAppender> {

        @PluginBuilderAttribute
        @Required
        private String fileName;

        @PluginBuilderAttribute
        private boolean append = true;

        @PluginBuilderAttribute
        private boolean locking;

        @PluginBuilderAttribute
        private boolean advertise;

        @PluginBuilderAttribute
        private String advertiseUri;

        @PluginBuilderAttribute
        private boolean createOnDemand;

        @PluginBuilderAttribute
        private String filePermissions;

        @PluginBuilderAttribute
        private String fileOwner;

        @PluginBuilderAttribute
        private String fileGroup;

        @Override
        public FileAppender build() {
            if (!isValid()) {
                return null;
            }
            boolean bufferedIo = isBufferedIo();
            final int bufferSize = getBufferSize();
            if (locking && bufferedIo) {
                LOGGER.warn("Locking and buffering are mutually exclusive. No buffering will occur for {}", fileName);
                bufferedIo = false;
            }
            if (!bufferedIo && bufferSize > 0) {
                LOGGER.warn("The bufferSize is set to {} but bufferedIo is false: {}", bufferSize, bufferedIo);
            }
            final Layout<? extends Serializable> layout = getOrCreateLayout();

            final FileManager manager = FileManager.getFileManager(
                    fileName,
                    append,
                    locking,
                    bufferedIo,
                    createOnDemand,
                    advertiseUri,
                    layout,
                    bufferSize,
                    filePermissions,
                    fileOwner,
                    fileGroup,
                    getConfiguration());
            if (manager == null) {
                return null;
            }

            return new FileAppender(
                    getName(),
                    layout,
                    getFilter(),
                    manager,
                    fileName,
                    isIgnoreExceptions(),
                    !bufferedIo || isImmediateFlush(),
                    advertise ? getConfiguration().getAdvertiser() : null,
                    getPropertyArray());
        }

        public String getAdvertiseUri() {
            return advertiseUri;
        }

        public String getFileName() {
            return fileName;
        }

        public boolean isAdvertise() {
            return advertise;
        }

        public boolean isAppend() {
            return append;
        }

        public boolean isCreateOnDemand() {
            return createOnDemand;
        }

        public boolean isLocking() {
            return locking;
        }

        public String getFilePermissions() {
            return filePermissions;
        }

        public String getFileOwner() {
            return fileOwner;
        }

        public String getFileGroup() {
            return fileGroup;
        }

        public B withAdvertise(final boolean advertise) {
            this.advertise = advertise;
            return asBuilder();
        }

        public B withAdvertiseUri(final String advertiseUri) {
            this.advertiseUri = advertiseUri;
            return asBuilder();
        }

        public B withAppend(final boolean append) {
            this.append = append;
            return asBuilder();
        }

        public B withFileName(final String fileName) {
            this.fileName = fileName;
            return asBuilder();
        }

        public B withCreateOnDemand(final boolean createOnDemand) {
            this.createOnDemand = createOnDemand;
            return asBuilder();
        }

        public B withLocking(final boolean locking) {
            this.locking = locking;
            return asBuilder();
        }

        public B withFilePermissions(final String filePermissions) {
            this.filePermissions = filePermissions;
            return asBuilder();
        }

        public B withFileOwner(final String fileOwner) {
            this.fileOwner = fileOwner;
            return asBuilder();
        }

        public B withFileGroup(final String fileGroup) {
            this.fileGroup = fileGroup;
            return asBuilder();
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
    public static <B extends Builder<B>> FileAppender createAppender(
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
        return FileAppender.<B>newBuilder()
                .withAdvertise(Boolean.parseBoolean(advertise))
                .withAdvertiseUri(advertiseUri)
                .withAppend(Booleans.parseBoolean(append, true))
                .withBufferedIo(Booleans.parseBoolean(bufferedIo, true))
                .withBufferSize(Integers.parseInt(bufferSizeStr, DEFAULT_BUFFER_SIZE))
                .setConfiguration(config)
                .withFileName(fileName)
                .setFilter(filter)
                .setIgnoreExceptions(Booleans.parseBoolean(ignoreExceptions, true))
                .withImmediateFlush(Booleans.parseBoolean(immediateFlush, true))
                .setLayout(layout)
                .withLocking(Boolean.parseBoolean(locking))
                .setName(name)
                .build();
        // @formatter:on
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final String fileName;

    private final Advertiser advertiser;

    private final Object advertisement;

    private FileAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final FileManager manager,
            final String filename,
            final boolean ignoreExceptions,
            final boolean immediateFlush,
            final Advertiser advertiser,
            final Property[] properties) {

        super(name, layout, filter, ignoreExceptions, immediateFlush, properties, manager);
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
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        super.stop(timeout, timeUnit, false);
        if (advertiser != null) {
            advertiser.unadvertise(advertisement);
        }
        setStopped();
        return true;
    }
}
