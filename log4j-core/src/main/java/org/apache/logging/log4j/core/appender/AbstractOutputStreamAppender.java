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
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.util.Constants;

/**
 * Appends log events as bytes to a byte output stream. The stream encoding is defined in the layout.
 *
 * @param <M> The kind of {@link OutputStreamManager} under management
 */
public abstract class AbstractOutputStreamAppender<M extends OutputStreamManager> extends AbstractAppender {

    /**
     * Subclasses can extend this abstract Builder.
     *
     * @param <B> The type to build.
     */
    public abstract static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B> {

        @PluginBuilderAttribute
        private boolean bufferedIo = true;

        @PluginBuilderAttribute
        private int bufferSize = Constants.ENCODER_BYTE_BUFFER_SIZE;

        @PluginBuilderAttribute
        private boolean immediateFlush = true;

        public int getBufferSize() {
            return bufferSize;
        }

        public boolean isBufferedIo() {
            return bufferedIo;
        }

        public boolean isImmediateFlush() {
            return immediateFlush;
        }

        public B setImmediateFlush(final boolean immediateFlush) {
            this.immediateFlush = immediateFlush;
            return asBuilder();
        }

        public B setBufferedIo(final boolean bufferedIo) {
            this.bufferedIo = bufferedIo;
            return asBuilder();
        }

        public B setBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return asBuilder();
        }

        @Deprecated
        public B withImmediateFlush(final boolean immediateFlush) {
            this.immediateFlush = immediateFlush;
            return asBuilder();
        }

        @Deprecated
        public B withBufferedIo(final boolean bufferedIo) {
            this.bufferedIo = bufferedIo;
            return asBuilder();
        }

        @Deprecated
        public B withBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return asBuilder();
        }
    }

    /**
     * Immediate flush means that the underlying writer or output stream will be flushed at the end of each append
     * operation. Immediate flush is slower but ensures that each append request is actually written. If
     * <code>immediateFlush</code> is set to {@code false}, then there is a good chance that the last few logs events
     * are not actually written to persistent media if and when the application crashes.
     */
    private final boolean immediateFlush;

    private final M manager;

    /**
     * Instantiates a WriterAppender and set the output destination to a new {@link java.io.OutputStreamWriter}
     * initialized with <code>os</code> as its {@link java.io.OutputStream}.
     *
     * @param name The name of the Appender.
     * @param layout The layout to format the message.
     * @param manager The OutputStreamManager.
     * @deprecated Use {@link #AbstractOutputStreamAppender(String, Layout, Filter, boolean, boolean, Property[], OutputStreamManager)}
     */
    @Deprecated
    protected AbstractOutputStreamAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final boolean ignoreExceptions,
            final boolean immediateFlush,
            final M manager) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
        this.manager = manager;
        this.immediateFlush = immediateFlush;
    }

    /**
     * Instantiates a WriterAppender and set the output destination to a new {@link java.io.OutputStreamWriter}
     * initialized with <code>os</code> as its {@link java.io.OutputStream}.
     *
     * @param name The name of the Appender.
     * @param layout The layout to format the message.
     * @param filter The filter to associate with the Appender.
     * @param ignoreExceptions If true, exceptions will be logged and suppressed.
     *                         If false errors will be logged and then passed to the application.
     * @param immediateFlush Underlying output stream will be flushed at the end of each append operation.
     * @param properties optional properties
     * @param manager The OutputStreamManager.
     */
    protected AbstractOutputStreamAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final boolean ignoreExceptions,
            final boolean immediateFlush,
            final Property[] properties,
            final M manager) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.manager = manager;
        this.immediateFlush = immediateFlush;
    }

    /**
     * Gets the immediate flush setting.
     *
     * @return immediate flush.
     */
    public boolean getImmediateFlush() {
        return immediateFlush;
    }

    /**
     * Gets the manager.
     *
     * @return the manager.
     */
    public M getManager() {
        return manager;
    }

    @Override
    public void start() {
        if (getLayout() == null) {
            LOGGER.error("No layout set for the appender named [" + getName() + "].");
        }
        if (manager == null) {
            LOGGER.error("No OutputStreamManager set for the appender named [" + getName() + "].");
        }
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        return stop(timeout, timeUnit, true);
    }

    @Override
    protected boolean stop(final long timeout, final TimeUnit timeUnit, final boolean changeLifeCycleState) {
        boolean stopped = super.stop(timeout, timeUnit, changeLifeCycleState);
        stopped &= manager.stop(timeout, timeUnit);
        if (changeLifeCycleState) {
            setStopped();
        }
        LOGGER.debug("Appender {} stopped with status {}", getName(), stopped);
        return stopped;
    }

    /**
     * Actual writing occurs here.
     * <p>
     * Most subclasses of <code>AbstractOutputStreamAppender</code> will need to override this method.
     * </p>
     *
     * @param event The LogEvent.
     */
    @Override
    public void append(final LogEvent event) {
        try {
            tryAppend(event);
        } catch (final AppenderLoggingException ex) {
            error("Unable to write to stream " + manager.getName() + " for appender " + getName(), event, ex);
            throw ex;
        }
    }

    private void tryAppend(final LogEvent event) {
        if (Constants.ENABLE_DIRECT_ENCODERS) {
            directEncodeEvent(event);
        } else {
            writeByteArrayToManager(event);
        }
    }

    protected void directEncodeEvent(final LogEvent event) {
        getLayout().encode(event, manager);
        if (this.immediateFlush || event.isEndOfBatch()) {
            manager.flush();
        }
    }

    protected void writeByteArrayToManager(final LogEvent event) {
        final byte[] bytes = getLayout().toByteArray(event);
        if (bytes != null && bytes.length > 0) {
            manager.write(bytes, this.immediateFlush || event.isEndOfBatch());
        }
    }
}
