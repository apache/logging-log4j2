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

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.StringLayout;

/**
 * Manages a Writer so that it can be shared by multiple Appenders and will
 * allow appenders to reconfigure without requiring a new writer.
 */
public class WriterManager extends AbstractManager {

    /**
     * Creates a Manager.
     *
     * @param name The name of the stream to manage.
     * @param data The data to pass to the Manager.
     * @param factory The factory to use to create the Manager.
     * @param <T> The type of the WriterManager.
     * @return A WriterManager.
     */
    public static <T> WriterManager getManager(
            final String name, final T data, final ManagerFactory<? extends WriterManager, T> factory) {
        return AbstractManager.getManager(name, factory, data);
    }

    protected final StringLayout layout;

    private volatile Writer writer;

    public WriterManager(
            final Writer writer, final String streamName, final StringLayout layout, final boolean writeHeader) {
        super(null, streamName);
        this.writer = writer;
        this.layout = layout;
        if (writeHeader && layout != null) {
            final byte[] header = layout.getHeader();
            if (header != null) {
                try {
                    this.writer.write(new String(header, layout.getCharset()));
                } catch (final IOException e) {
                    logError("Unable to write header", e);
                }
            }
        }
    }

    protected synchronized void closeWriter() {
        final Writer w = writer; // access volatile field only once per method
        try {
            w.close();
        } catch (final IOException ex) {
            logError("Unable to close stream", ex);
        }
    }

    /**
     * Flushes any buffers.
     */
    public synchronized void flush() {
        try {
            writer.flush();
        } catch (final IOException ex) {
            final String msg = "Error flushing stream " + getName();
            throw new AppenderLoggingException(msg, ex);
        }
    }

    protected Writer getWriter() {
        return writer;
    }

    /**
     * Returns the status of the stream.
     * @return true if the stream is open, false if it is not.
     */
    public boolean isOpen() {
        return getCount() > 0;
    }

    /**
     * Default hook to write footer during close.
     */
    @Override
    public boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        writeFooter();
        closeWriter();
        return true;
    }

    protected void setWriter(final Writer writer) {
        final byte[] header = layout.getHeader();
        if (header != null) {
            try {
                writer.write(new String(header, layout.getCharset()));
                this.writer = writer; // only update field if writer.write() succeeded
            } catch (final IOException ioe) {
                logError("Unable to write header", ioe);
            }
        } else {
            this.writer = writer;
        }
    }

    /**
     * Some output streams synchronize writes while others do not. Synchronizing here insures that
     * log events won't be intertwined.
     * @param str the string to write
     * @throws AppenderLoggingException if an error occurs.
     */
    protected synchronized void write(final String str) {
        try {
            writer.write(str);
        } catch (final IOException ex) {
            final String msg = "Error writing to stream " + getName();
            throw new AppenderLoggingException(msg, ex);
        }
    }

    /**
     * Writes the footer.
     */
    protected void writeFooter() {
        if (layout == null) {
            return;
        }
        final byte[] footer = layout.getFooter();
        if (footer != null && footer.length > 0) {
            write(new String(footer, layout.getCharset()));
        }
    }
}
