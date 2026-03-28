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
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;

/**
 * An appender that writes to files and can roll over at intervals.
 */
@Plugin(
        name = RollingFileAppender.PLUGIN_NAME,
        category = Core.CATEGORY_NAME,
        elementType = Appender.ELEMENT_TYPE,
        printObject = true)
public final class RollingFileAppender extends AbstractOutputStreamAppender<RollingFileManager> {

    public static final String PLUGIN_NAME = "RollingFile";

    /**
     * Builds FileAppender instances.
     *
     * @param <B>
     *            The type to build
     * @since 2.7
     */
    public static class Builder<B extends Builder<B>> extends AbstractOutputStreamAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<RollingFileAppender> {

        @PluginBuilderAttribute
        private String fileName;

        @PluginBuilderAttribute
        @Required
        private String filePattern;

        @PluginBuilderAttribute
        private boolean append = true;

        @PluginBuilderAttribute
        private boolean locking;

        @PluginElement("Policy")
        @Required
        private TriggeringPolicy policy;

        @PluginElement("Strategy")
        private RolloverStrategy strategy;

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
        public RollingFileAppender build() {
            if (!isValid()) {
                return null;
            }
            // Even though some variables may be annotated with @Required, we must still perform validation here for
            // call sites that build builders programmatically.
            final boolean isBufferedIo = isBufferedIo();
            final int bufferSize = getBufferSize();

            if (!isBufferedIo && bufferSize > 0) {
                LOGGER.warn(
                        "RollingFileAppender '{}': The bufferSize is set to {} but bufferedIO is not true",
                        getName(),
                        bufferSize);
            }

            if (strategy == null) {
                if (fileName != null) {
                    strategy = DefaultRolloverStrategy.newBuilder()
                            .setCompressionLevelStr(String.valueOf(Deflater.DEFAULT_COMPRESSION))
                            .setConfig(getConfiguration())
                            .build();
                } else {
                    strategy = DirectWriteRolloverStrategy.newBuilder()
                            .setCompressionLevelStr(String.valueOf(Deflater.DEFAULT_COMPRESSION))
                            .setConfig(getConfiguration())
                            .build();
                }
            } else if (fileName == null && !(strategy instanceof DirectFileRolloverStrategy)) {
                LOGGER.error(
                        "RollingFileAppender '{}': When no file name is provided a {} must be configured",
                        getName(),
                        DirectFileRolloverStrategy.class.getSimpleName());
                return null;
            }

            final Layout<? extends Serializable> layout = getOrCreateLayout();
            final RollingFileManager manager = RollingFileManager.getFileManager(
                    fileName,
                    filePattern,
                    append,
                    isBufferedIo,
                    policy,
                    strategy,
                    advertiseUri,
                    layout,
                    bufferSize,
                    isImmediateFlush(),
                    createOnDemand,
                    filePermissions,
                    fileOwner,
                    fileGroup,
                    getConfiguration());
            if (manager == null) {
                return null;
            }

            manager.initialize();

            return new RollingFileAppender(
                    getName(),
                    layout,
                    getFilter(),
                    manager,
                    fileName,
                    filePattern,
                    isIgnoreExceptions(),
                    !isBufferedIo || isImmediateFlush(),
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

        /**
         * @since 2.26.0
         */
        public B setAdvertise(final boolean advertise) {
            this.advertise = advertise;
            return asBuilder();
        }

        /**
         * @since 2.26.0
         */
        public B setAdvertiseUri(final String advertiseUri) {
            this.advertiseUri = advertiseUri;
            return asBuilder();
        }

        /**
         * @since 2.26.0
         */
        public B setAppend(final boolean append) {
            this.append = append;
            return asBuilder();
        }

        /**
         * @since 2.26.0
         */
        public B setFileName(final String fileName) {
            this.fileName = fileName;
            return asBuilder();
        }

        /**
         * @since 2.26.0
         */
        public B setCreateOnDemand(final boolean createOnDemand) {
            this.createOnDemand = createOnDemand;
            return asBuilder();
        }

        /**
         * @since 2.26.0
         */
        public B setLocking(final boolean locking) {
            this.locking = locking;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setAdvertise(boolean)}.
         */
        @Deprecated
        public B withAdvertise(final boolean advertise) {
            this.advertise = advertise;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setAdvertiseUri(String)}.
         */
        @Deprecated
        public B withAdvertiseUri(final String advertiseUri) {
            this.advertiseUri = advertiseUri;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setAppend(boolean)}.
         */
        @Deprecated
        public B withAppend(final boolean append) {
            this.append = append;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setFileName(String)}.
         */
        @Deprecated
        public B withFileName(final String fileName) {
            this.fileName = fileName;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setCreateOnDemand(boolean)}.
         */
        @Deprecated
        public B withCreateOnDemand(final boolean createOnDemand) {
            this.createOnDemand = createOnDemand;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setLocking(boolean)}.
         */
        @Deprecated
        public B withLocking(final boolean locking) {
            this.locking = locking;
            return asBuilder();
        }

        public String getFilePattern() {
            return filePattern;
        }

        public TriggeringPolicy getPolicy() {
            return policy;
        }

        public RolloverStrategy getStrategy() {
            return strategy;
        }

        /**
         * @since 2.26.0
         */
        public B setFilePattern(final String filePattern) {
            this.filePattern = filePattern;
            return asBuilder();
        }

        /**
         * @since 2.26.0
         */
        public B setPolicy(final TriggeringPolicy policy) {
            this.policy = policy;
            return asBuilder();
        }

        /**
         * @since 2.26.0
         */
        public B setStrategy(final RolloverStrategy strategy) {
            this.strategy = strategy;
            return asBuilder();
        }

        /**
         * @since 2.26.0
         */
        public B setFilePermissions(final String filePermissions) {
            this.filePermissions = filePermissions;
            return asBuilder();
        }

        /**
         * @since 2.26.0
         */
        public B setFileOwner(final String fileOwner) {
            this.fileOwner = fileOwner;
            return asBuilder();
        }

        /**
         * @since 2.26.0
         */
        public B setFileGroup(final String fileGroup) {
            this.fileGroup = fileGroup;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setFilePattern(String)}.
         */
        @Deprecated
        public B withFilePattern(final String filePattern) {
            this.filePattern = filePattern;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setPolicy(TriggeringPolicy)}.
         */
        @Deprecated
        public B withPolicy(final TriggeringPolicy policy) {
            this.policy = policy;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setStrategy(RolloverStrategy)}.
         */
        @Deprecated
        public B withStrategy(final RolloverStrategy strategy) {
            this.strategy = strategy;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setFilePermissions(String)}.
         */
        @Deprecated
        public B withFilePermissions(final String filePermissions) {
            this.filePermissions = filePermissions;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setFileOwner(String)}.
         */
        @Deprecated
        public B withFileOwner(final String fileOwner) {
            this.fileOwner = fileOwner;
            return asBuilder();
        }

        /**
         * @deprecated since 2.26.0 use {@link #setFileGroup(String)}.
         */
        @Deprecated
        public B withFileGroup(final String fileGroup) {
            this.fileGroup = fileGroup;
            return asBuilder();
        }
    }

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final String fileName;
    private final String filePattern;
    private Object advertisement;
    private final Advertiser advertiser;

    private RollingFileAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final RollingFileManager manager,
            final String fileName,
            final String filePattern,
            final boolean ignoreExceptions,
            final boolean immediateFlush,
            final Advertiser advertiser,
            final Property[] properties) {
        super(name, layout, filter, ignoreExceptions, immediateFlush, properties, manager);
        if (advertiser != null) {
            final Map<String, String> configuration = new HashMap<>(layout.getContentFormat());
            configuration.put("contentType", layout.getContentType());
            configuration.put("name", name);
            advertisement = advertiser.advertise(configuration);
        }
        this.fileName = fileName;
        this.filePattern = filePattern;
        this.advertiser = advertiser;
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        final boolean stopped = super.stop(timeout, timeUnit, false);
        if (advertiser != null) {
            advertiser.unadvertise(advertisement);
        }
        setStopped();
        return stopped;
    }

    /**
     * Writes the log entry rolling over the file when required.
     *
     * @param event The LogEvent.
     */
    @Override
    public void append(final LogEvent event) {
        getManager().checkRollover(event);
        super.append(event);
    }

    /**
     * Returns the File name for the Appender.
     * @return The file name.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the file pattern used when rolling over.
     * @return The file pattern.
     */
    public String getFilePattern() {
        return filePattern;
    }

    /**
     * Returns the triggering policy.
     * @param <T> TriggeringPolicy type
     * @return The TriggeringPolicy
     */
    public <T extends TriggeringPolicy> T getTriggeringPolicy() {
        return getManager().getTriggeringPolicy();
    }

    /**
     * Creates a RollingFileAppender.
     * @param fileName The name of the file that is actively written to. (required).
     * @param filePattern The pattern of the file name to use on rollover. (required).
     * @param append If true, events are appended to the file. If false, the file
     * is overwritten when opened. Defaults to "true"
     * @param name The name of the Appender (required).
     * @param bufferedIO When true, I/O will be buffered. Defaults to "true".
     * @param bufferSizeStr buffer size for buffered IO (default is 8192).
     * @param immediateFlush When true, events are immediately flushed. Defaults to "true".
     * @param policy The triggering policy. (required).
     * @param strategy The rollover strategy. Defaults to DefaultRolloverStrategy.
     * @param layout The layout to use (defaults to the default PatternLayout).
     * @param filter The Filter or null.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param advertise "true" if the appender configuration should be advertised, "false" otherwise.
     * @param advertiseUri The advertised URI which can be used to retrieve the file contents.
     * @param config The Configuration.
     * @return A RollingFileAppender.
     * @deprecated Use {@link #newBuilder()}.
     */
    @Deprecated
    public static <B extends Builder<B>> RollingFileAppender createAppender(
            // @formatter:off
            final String fileName,
            final String filePattern,
            final String append,
            final String name,
            final String bufferedIO,
            final String bufferSizeStr,
            final String immediateFlush,
            final TriggeringPolicy policy,
            final RolloverStrategy strategy,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final String ignore,
            final String advertise,
            final String advertiseUri,
            final Configuration config) {
        // @formatter:on
        final int bufferSize = Integers.parseInt(bufferSizeStr, DEFAULT_BUFFER_SIZE);
        // @formatter:off
        return RollingFileAppender.<B>newBuilder()
                .setAdvertise(Boolean.parseBoolean(advertise))
                .setAdvertiseUri(advertiseUri)
                .setAppend(Booleans.parseBoolean(append, true))
                .setBufferedIo(Booleans.parseBoolean(bufferedIO, true))
                .setBufferSize(bufferSize)
                .setConfiguration(config)
                .setFileName(fileName)
                .setFilePattern(filePattern)
                .setFilter(filter)
                .setIgnoreExceptions(Booleans.parseBoolean(ignore, true))
                .setImmediateFlush(Booleans.parseBoolean(immediateFlush, true))
                .setLayout(layout)
                .setCreateOnDemand(false)
                .setLocking(false)
                .setName(name)
                .setPolicy(policy)
                .setStrategy(strategy)
                .build();
        // @formatter:on
    }

    /**
     * Creates a new Builder.
     *
     * @return a new Builder.
     * @since 2.7
     */
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }
}
