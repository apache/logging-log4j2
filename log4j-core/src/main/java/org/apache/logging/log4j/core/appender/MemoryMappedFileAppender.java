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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Memory Mapped File Appender.
 *
 * @since 2.1
 */
@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin("MemoryMappedFile")
public final class MemoryMappedFileAppender extends AbstractOutputStreamAppender<MemoryMappedFileManager> {

    /**
     * Builds RandomAccessFileAppender instances.
     *
     * @param <B>
     *            The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.plugins.util.Builder<MemoryMappedFileAppender> {

        @PluginBuilderAttribute("fileName")
        private String fileName;

        @PluginBuilderAttribute("append")
        private boolean append = true;

        @PluginBuilderAttribute("regionLength")
        private int regionLength = MemoryMappedFileManager.DEFAULT_REGION_LENGTH;

        @PluginBuilderAttribute("advertise")
        private boolean advertise;

        @PluginBuilderAttribute("advertiseURI")
        private String advertiseURI;

        @Override
        public MemoryMappedFileAppender build() {
            final String name = getName();
            final int actualRegionLength = determineValidRegionLength(name, regionLength);

            if (name == null) {
                LOGGER.error("No name provided for MemoryMappedFileAppender");
                return null;
            }

            if (fileName == null) {
                LOGGER.error("No filename provided for MemoryMappedFileAppender with name " + name);
                return null;
            }
            final Layout<? extends Serializable> layout = getOrCreateLayout();
            final MemoryMappedFileManager manager = MemoryMappedFileManager.getFileManager(fileName, append, isImmediateFlush(),
                    actualRegionLength, advertiseURI, layout);
            if (manager == null) {
                return null;
            }

            return new MemoryMappedFileAppender(name, layout, getFilter(), manager, fileName, isIgnoreExceptions(), false,
                    advertise ? getConfiguration().getAdvertiser() : null, getPropertyArray());
        }

        public B setFileName(final String fileName) {
            this.fileName = fileName;
            return asBuilder();
        }

        public B setAppend(final boolean append) {
            this.append = append;
            return asBuilder();
        }

        public B setRegionLength(final int regionLength) {
            this.regionLength = regionLength;
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

    private static final int BIT_POSITION_1GB = 30; // 2^30 ~= 1GB
    private static final int MAX_REGION_LENGTH = 1 << BIT_POSITION_1GB;
    private static final int MIN_REGION_LENGTH = 256;

    private final String fileName;
    private Object advertisement;
    private final Advertiser advertiser;

    private MemoryMappedFileAppender(final String name, final Layout<? extends Serializable> layout,
            final Filter filter, final MemoryMappedFileManager manager, final String filename,
            final boolean ignoreExceptions, final boolean immediateFlush, final Advertiser advertiser, final Property[] properties) {
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
     * Returns the length of the memory mapped region.
     *
     * @return the length of the memory mapped region
     */
    public int getRegionLength() {
        return getManager().getRegionLength();
    }

    @PluginFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    /**
     * Converts the specified region length to a valid value.
     */
    private static int determineValidRegionLength(final String name, final int regionLength) {
        if (regionLength > MAX_REGION_LENGTH) {
            LOGGER.info("MemoryMappedAppender[{}] Reduced region length from {} to max length: {}", name, regionLength,
                    MAX_REGION_LENGTH);
            return MAX_REGION_LENGTH;
        }
        if (regionLength < MIN_REGION_LENGTH) {
            LOGGER.info("MemoryMappedAppender[{}] Expanded region length from {} to min length: {}", name, regionLength,
                    MIN_REGION_LENGTH);
            return MIN_REGION_LENGTH;
        }
        final int result = Integers.ceilingNextPowerOfTwo(regionLength);
        if (regionLength != result) {
            LOGGER.info("MemoryMappedAppender[{}] Rounded up region length from {} to next power of two: {}", name,
                    regionLength, result);
        }
        return result;
    }
}
