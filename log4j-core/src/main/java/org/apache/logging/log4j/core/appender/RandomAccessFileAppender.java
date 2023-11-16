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
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;

/**
 * File Appender.
 */
@Plugin(
        name = "RandomAccessFile",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE,
        printObject = true)
public final class RandomAccessFileAppender extends AbstractOutputStreamAppender<RandomAccessFileManager> {

    /**
     * Builds RandomAccessFileAppender instances.
     *
     * @param <B>
     *            The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<RandomAccessFileAppender> {

        @PluginBuilderAttribute("fileName")
        private String fileName;

        @PluginBuilderAttribute("append")
        private boolean append = true;

        @PluginBuilderAttribute("advertise")
        private boolean advertise;

        @PluginBuilderAttribute("advertiseURI")
        private String advertiseURI;

        public Builder() {
            this.withBufferSize(RandomAccessFileManager.DEFAULT_BUFFER_SIZE);
        }

        @Override
        public RandomAccessFileAppender build() {
            final String name = getName();
            if (name == null) {
                LOGGER.error("No name provided for RandomAccessFileAppender");
                return null;
            }

            if (fileName == null) {
                LOGGER.error("No filename provided for RandomAccessFileAppender with name {}", name);
                return null;
            }
            final Layout<? extends Serializable> layout = getOrCreateLayout();
            final boolean immediateFlush = isImmediateFlush();
            final RandomAccessFileManager manager = RandomAccessFileManager.getFileManager(
                    fileName, append, immediateFlush, getBufferSize(), advertiseURI, layout, null);
            if (manager == null) {
                return null;
            }

            return new RandomAccessFileAppender(
                    name,
                    layout,
                    getFilter(),
                    manager,
                    fileName,
                    isIgnoreExceptions(),
                    immediateFlush,
                    advertise ? getConfiguration().getAdvertiser() : null,
                    getPropertyArray());
        }

        public B setFileName(final String fileName) {
            this.fileName = fileName;
            return asBuilder();
        }

        public B setAppend(final boolean append) {
            this.append = append;
            return asBuilder();
        }

        public B setAdvertise(final boolean advertise) {
            this.advertise = advertise;
            return asBuilder();
        }

        public B setAdvertiseURI(final String advertiseURI) {
            this.advertiseURI = advertiseURI;
            return asBuilder();
        }
    }

    private final String fileName;
    private Object advertisement;
    private final Advertiser advertiser;

    private RandomAccessFileAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final RandomAccessFileManager manager,
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
        }
        this.fileName = filename;
        this.advertiser = advertiser;
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

    /**
     * Returns the file name this appender is associated with.
     *
     * @return The File name.
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Returns the size of the file manager's buffer.
     * @return the buffer size
     */
    public int getBufferSize() {
        return getManager().getBufferSize();
    }

    // difference from standard File Appender:
    // locking is not supported and buffering cannot be switched off
    /**
     * Create a File Appender.
     *
     * @param fileName The name and path of the file.
     * @param append "True" if the file should be appended to, "false" if it
     *            should be overwritten. The default is "true".
     * @param name The name of the Appender.
     * @param immediateFlush "true" if the contents should be flushed on every
     *            write, "false" otherwise. The default is "true".
     * @param bufferSizeStr The buffer size, defaults to {@value RandomAccessFileManager#DEFAULT_BUFFER_SIZE}.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param layout The layout to use to format the event. If no layout is
     *            provided the default PatternLayout will be used.
     * @param filter The filter, if any, to use.
     * @param advertise "true" if the appender configuration should be
     *            advertised, "false" otherwise.
     * @param advertiseURI The advertised URI which can be used to retrieve the
     *            file contents.
     * @param configuration The Configuration.
     * @return The FileAppender.
     * @deprecated Use {@link #newBuilder()}.
     */
    @Deprecated
    public static <B extends Builder<B>> RandomAccessFileAppender createAppender(
            final String fileName,
            final String append,
            final String name,
            final String immediateFlush,
            final String bufferSizeStr,
            final String ignore,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final String advertise,
            final String advertiseURI,
            final Configuration configuration) {

        final boolean isAppend = Booleans.parseBoolean(append, true);
        final boolean isFlush = Booleans.parseBoolean(immediateFlush, true);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final boolean isAdvertise = Boolean.parseBoolean(advertise);
        final int bufferSize = Integers.parseInt(bufferSizeStr, RandomAccessFileManager.DEFAULT_BUFFER_SIZE);

        return RandomAccessFileAppender.<B>newBuilder()
                .setAdvertise(isAdvertise)
                .setAdvertiseURI(advertiseURI)
                .setAppend(isAppend)
                .withBufferSize(bufferSize)
                .setConfiguration(configuration)
                .setFileName(fileName)
                .setFilter(filter)
                .setIgnoreExceptions(ignoreExceptions)
                .withImmediateFlush(isFlush)
                .setLayout(layout)
                .setName(name)
                .build();
    }

    /**
     * Creates a builder for a RandomAccessFileAppender.
     * @return a builder for a RandomAccessFileAppender.
     */
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }
}
