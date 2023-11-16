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
import java.util.zip.Deflater;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.DirectFileRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.DirectWriteRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RollingRandomAccessFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;

/**
 * An appender that writes to random access files and can roll over at
 * intervals.
 */
@Plugin(
        name = "RollingRandomAccessFile",
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE,
        printObject = true)
public final class RollingRandomAccessFileAppender
        extends AbstractOutputStreamAppender<RollingRandomAccessFileManager> {

    public static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<RollingRandomAccessFileAppender> {

        public Builder() {
            withBufferSize(RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE);
            setIgnoreExceptions(true);
            withImmediateFlush(true);
        }

        @PluginBuilderAttribute("fileName")
        private String fileName;

        @PluginBuilderAttribute("filePattern")
        private String filePattern;

        @PluginBuilderAttribute("append")
        private boolean append = true;

        @PluginElement("Policy")
        private TriggeringPolicy policy;

        @PluginElement("Strategy")
        private RolloverStrategy strategy;

        @PluginBuilderAttribute("advertise")
        private boolean advertise;

        @PluginBuilderAttribute("advertiseURI")
        private String advertiseURI;

        @PluginBuilderAttribute
        private String filePermissions;

        @PluginBuilderAttribute
        private String fileOwner;

        @PluginBuilderAttribute
        private String fileGroup;

        @Override
        public RollingRandomAccessFileAppender build() {
            final String name = getName();
            if (name == null) {
                LOGGER.error("No name provided for FileAppender");
                return null;
            }

            if (strategy == null) {
                if (fileName != null) {
                    strategy = DefaultRolloverStrategy.newBuilder()
                            .withCompressionLevelStr(String.valueOf(Deflater.DEFAULT_COMPRESSION))
                            .withConfig(getConfiguration())
                            .build();
                } else {
                    strategy = DirectWriteRolloverStrategy.newBuilder()
                            .withCompressionLevelStr(String.valueOf(Deflater.DEFAULT_COMPRESSION))
                            .withConfig(getConfiguration())
                            .build();
                }
            } else if (fileName == null && !(strategy instanceof DirectFileRolloverStrategy)) {
                LOGGER.error(
                        "RollingFileAppender '{}': When no file name is provided a DirectFileRolloverStrategy must be configured");
                return null;
            }

            if (filePattern == null) {
                LOGGER.error("No filename pattern provided for FileAppender with name " + name);
                return null;
            }

            if (policy == null) {
                LOGGER.error("A TriggeringPolicy must be provided");
                return null;
            }

            final Layout<? extends Serializable> layout = getOrCreateLayout();

            final boolean immediateFlush = isImmediateFlush();
            final int bufferSize = getBufferSize();
            final RollingRandomAccessFileManager manager =
                    RollingRandomAccessFileManager.getRollingRandomAccessFileManager(
                            fileName,
                            filePattern,
                            append,
                            immediateFlush,
                            bufferSize,
                            policy,
                            strategy,
                            advertiseURI,
                            layout,
                            filePermissions,
                            fileOwner,
                            fileGroup,
                            getConfiguration());
            if (manager == null) {
                return null;
            }

            manager.initialize();

            return new RollingRandomAccessFileAppender(
                    name,
                    layout,
                    getFilter(),
                    manager,
                    fileName,
                    filePattern,
                    isIgnoreExceptions(),
                    immediateFlush,
                    bufferSize,
                    advertise ? getConfiguration().getAdvertiser() : null,
                    getPropertyArray());
        }

        public B withFileName(final String fileName) {
            this.fileName = fileName;
            return asBuilder();
        }

        public B withFilePattern(final String filePattern) {
            this.filePattern = filePattern;
            return asBuilder();
        }

        public B withAppend(final boolean append) {
            this.append = append;
            return asBuilder();
        }

        public B withPolicy(final TriggeringPolicy policy) {
            this.policy = policy;
            return asBuilder();
        }

        public B withStrategy(final RolloverStrategy strategy) {
            this.strategy = strategy;
            return asBuilder();
        }

        public B withAdvertise(final boolean advertise) {
            this.advertise = advertise;
            return asBuilder();
        }

        public B withAdvertiseURI(final String advertiseURI) {
            this.advertiseURI = advertiseURI;
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

    private final String fileName;
    private final String filePattern;
    private final Object advertisement;
    private final Advertiser advertiser;

    private RollingRandomAccessFileAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final RollingRandomAccessFileManager manager,
            final String fileName,
            final String filePattern,
            final boolean ignoreExceptions,
            final boolean immediateFlush,
            final int bufferSize,
            final Advertiser advertiser,
            final Property[] properties) {
        super(name, layout, filter, ignoreExceptions, immediateFlush, properties, manager);
        if (advertiser != null) {
            final Map<String, String> configuration = new HashMap<>(layout.getContentFormat());
            configuration.put("contentType", layout.getContentType());
            configuration.put("name", name);
            advertisement = advertiser.advertise(configuration);
        } else {
            advertisement = null;
        }
        this.fileName = fileName;
        this.filePattern = filePattern;
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
     * Write the log entry rolling over the file when required.
     *
     * @param event The LogEvent.
     */
    @Override
    public void append(final LogEvent event) {
        final RollingRandomAccessFileManager manager = getManager();
        manager.checkRollover(event);

        // LOG4J2-1292 utilize gc-free Layout.encode() method: taken care of in superclass
        super.append(event);
    }

    /**
     * Returns the File name for the Appender.
     *
     * @return The file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the file pattern used when rolling over.
     *
     * @return The file pattern.
     */
    public String getFilePattern() {
        return filePattern;
    }

    /**
     * Returns the size of the file manager's buffer.
     * @return the buffer size
     */
    public int getBufferSize() {
        return getManager().getBufferSize();
    }

    /**
     * Create a RollingRandomAccessFileAppender.
     *
     * @param fileName The name of the file that is actively written to.
     *            (required).
     * @param filePattern The pattern of the file name to use on rollover.
     *            (required).
     * @param append If true, events are appended to the file. If false, the
     *            file is overwritten when opened. Defaults to "true"
     * @param name The name of the Appender (required).
     * @param immediateFlush When true, events are immediately flushed. Defaults
     *            to "true".
     * @param bufferSizeStr The buffer size, defaults to {@value RollingRandomAccessFileManager#DEFAULT_BUFFER_SIZE}.
     * @param policy The triggering policy. (required).
     * @param strategy The rollover strategy. Defaults to
     *            DefaultRolloverStrategy.
     * @param layout The layout to use (defaults to the default PatternLayout).
     * @param filter The Filter or null.
     * @param ignoreExceptions If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param advertise "true" if the appender configuration should be
     *            advertised, "false" otherwise.
     * @param advertiseURI The advertised URI which can be used to retrieve the
     *            file contents.
     * @param configuration The Configuration.
     * @return A RollingRandomAccessFileAppender.
     * @deprecated Use {@link #newBuilder()}.
     */
    @Deprecated
    public static <B extends Builder<B>> RollingRandomAccessFileAppender createAppender(
            final String fileName,
            final String filePattern,
            final String append,
            final String name,
            final String immediateFlush,
            final String bufferSizeStr,
            final TriggeringPolicy policy,
            final RolloverStrategy strategy,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final String ignoreExceptions,
            final String advertise,
            final String advertiseURI,
            final Configuration configuration) {

        final boolean isAppend = Booleans.parseBoolean(append, true);
        final boolean isIgnoreExceptions = Booleans.parseBoolean(ignoreExceptions, true);
        final boolean isImmediateFlush = Booleans.parseBoolean(immediateFlush, true);
        final boolean isAdvertise = Boolean.parseBoolean(advertise);
        final int bufferSize = Integers.parseInt(bufferSizeStr, RollingRandomAccessFileManager.DEFAULT_BUFFER_SIZE);

        return RollingRandomAccessFileAppender.<B>newBuilder()
                .withAdvertise(isAdvertise)
                .withAdvertiseURI(advertiseURI)
                .withAppend(isAppend)
                .withBufferSize(bufferSize)
                .setConfiguration(configuration)
                .withFileName(fileName)
                .withFilePattern(filePattern)
                .setFilter(filter)
                .setIgnoreExceptions(isIgnoreExceptions)
                .withImmediateFlush(isImmediateFlush)
                .setLayout(layout)
                .setName(name)
                .withPolicy(policy)
                .withStrategy(strategy)
                .build();
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }
}
