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
import org.apache.logging.log4j.core.LogEvent;
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
 * Memory Mapped File Appender.
 * 
 * @since 2.1
 */
@Plugin(name = "MemoryMappedFile", category = "Core", elementType = "appender", printObject = true)
public final class MemoryMappedFileAppender extends AbstractOutputStreamAppender<MemoryMappedFileManager> {

    private static final long serialVersionUID = 1L;

    private static final int MAX_REGION_LENGTH = 1 << 30; // 1GB
    private static final int MIN_REGION_LENGTH = 256;

    private final String fileName;
    private Object advertisement;
    private final Advertiser advertiser;

    private MemoryMappedFileAppender(final String name, final Layout<? extends Serializable> layout,
            final Filter filter, final MemoryMappedFileManager manager, final String filename,
            final boolean ignoreExceptions, final boolean immediateFlush, final Advertiser advertiser) {
        super(name, layout, filter, ignoreExceptions, immediateFlush, manager);
        if (advertiser != null) {
            final Map<String, String> configuration = new HashMap<String, String>(layout.getContentFormat());
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
     * Write the log entry rolling over the file when required.
     *
     * @param event The LogEvent.
     */
    @Override
    public void append(final LogEvent event) {

        // Leverage the nice batching behaviour of async Loggers/Appenders:
        // we can signal the file manager that it needs to flush the buffer
        // to disk at the end of a batch.
        // From a user's point of view, this means that all log events are
        // _always_ available in the log file, without incurring the overhead
        // of immediateFlush=true.
        getManager().setEndOfBatch(event.isEndOfBatch());
        super.append(event);
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

    /**
     * Create a Memory Mapped File Appender.
     *
     * @param fileName The name and path of the file.
     * @param append "True" if the file should be appended to, "false" if it should be overwritten. The default is
     *            "true".
     * @param name The name of the Appender.
     * @param immediateFlush "true" if the contents should be flushed on every write, "false" otherwise. The default is
     *            "true".
     * @param regionLengthStr The buffer size, defaults to {@value MemoryMappedFileManager#DEFAULT_REGION_LENGTH}.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise they
     *            are propagated to the caller.
     * @param layout The layout to use to format the event. If no layout is provided the default PatternLayout will be
     *            used.
     * @param filter The filter, if any, to use.
     * @param advertise "true" if the appender configuration should be advertised, "false" otherwise.
     * @param advertiseURI The advertised URI which can be used to retrieve the file contents.
     * @param config The Configuration.
     * @return The FileAppender.
     */
    @PluginFactory
    public static MemoryMappedFileAppender createAppender(
// @formatter:off
            @PluginAttribute("fileName") final String fileName, //
            @PluginAttribute("append") final String append, //
            @PluginAttribute("name") final String name, //
            @PluginAttribute("immediateFlush") final String immediateFlush, //
            @PluginAttribute("regionLength") final String regionLengthStr, //
            @PluginAttribute("ignoreExceptions") final String ignore, //
            @PluginElement("Layout") Layout<? extends Serializable> layout, //
            @PluginElement("Filter") final Filter filter, //
            @PluginAttribute("advertise") final String advertise, //
            @PluginAttribute("advertiseURI") final String advertiseURI, //
            @PluginConfiguration final Configuration config) {
        // @formatter:on

        final boolean isAppend = Booleans.parseBoolean(append, true);
        final boolean isForce = Booleans.parseBoolean(immediateFlush, false);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final boolean isAdvertise = Boolean.parseBoolean(advertise);
        final int regionLength = Integers.parseInt(regionLengthStr, MemoryMappedFileManager.DEFAULT_REGION_LENGTH);
        final int actualRegionLength = determineValidRegionLength(name, regionLength);

        if (name == null) {
            LOGGER.error("No name provided for MemoryMappedFileAppender");
            return null;
        }

        if (fileName == null) {
            LOGGER.error("No filename provided for MemoryMappedFileAppender with name " + name);
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        final MemoryMappedFileManager manager = MemoryMappedFileManager.getFileManager(fileName, isAppend, isForce,
                actualRegionLength, advertiseURI, layout);
        if (manager == null) {
            return null;
        }

        return new MemoryMappedFileAppender(name, layout, filter, manager, fileName, ignoreExceptions, isForce,
                isAdvertise ? config.getAdvertiser() : null);
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
