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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

/**
 * File Appender.
 */
@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin("RandomAccessFile")
public final class RandomAccessFileAppender extends AbstractOutputStreamAppender<RandomAccessFileManager> {

    /**
     * Builds RandomAccessFileAppender instances.
     */
    public static class Builder extends AbstractOutputStreamAppender.Builder<Builder>
            implements org.apache.logging.log4j.plugins.util.Builder<RandomAccessFileAppender> {

        private String fileName;
        private boolean append = true;
        private boolean advertise;
        private String advertiseURI;

        public Builder() {
            setBufferSize(RandomAccessFileManager.DEFAULT_BUFFER_SIZE);
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
            final Layout layout = getOrCreateLayout();
            final boolean immediateFlush = isImmediateFlush();
            final RandomAccessFileManager manager = RandomAccessFileManager.getFileManager(fileName, append,
                    immediateFlush, getBufferSize(), advertiseURI, layout, null);
            if (manager == null) {
                return null;
            }

            return new RandomAccessFileAppender(name, layout, getFilter(), manager, fileName, isIgnoreExceptions(),
                    immediateFlush, advertise ? getConfiguration().getAdvertiser() : null);
        }

        public String getFileName() {
            return fileName;
        }

        public boolean isAppend() {
            return append;
        }

        public boolean isAdvertise() {
            return advertise;
        }

        public String getAdvertiseURI() {
            return advertiseURI;
        }

        public Builder setFileName(@PluginAttribute final String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder setAppend(@PluginAttribute(defaultBoolean = true) final boolean append) {
            this.append = append;
            return this;
        }

        public Builder setAdvertise(@PluginAttribute final boolean advertise) {
            this.advertise = advertise;
            return this;
        }

        public Builder setAdvertiseURI(@PluginAttribute String advertiseURI) {
            this.advertiseURI = advertiseURI;
            return this;
        }

    }

    private final String fileName;
    private Object advertisement;
    private final Advertiser advertiser;

    private RandomAccessFileAppender(final String name, final Layout layout,
            final Filter filter, final RandomAccessFileManager manager, final String filename,
            final boolean ignoreExceptions, final boolean immediateFlush, final Advertiser advertiser) {

        super(name, layout, filter, ignoreExceptions, immediateFlush, null, manager);
        if (advertiser != null) {
            final Map<String, String> configuration = new HashMap<>(
                    layout.getContentFormat());
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

    /**
     * Creates a builder for a RandomAccessFileAppender.
     * @return a builder for a RandomAccessFileAppender.
     */
    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

}
