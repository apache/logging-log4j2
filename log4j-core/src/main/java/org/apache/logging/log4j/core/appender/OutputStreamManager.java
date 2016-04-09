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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.util.Constants;

/**
 * Manages an OutputStream so that it can be shared by multiple Appenders and will
 * allow appenders to reconfigure without requiring a new stream.
 */
public class OutputStreamManager extends AbstractManager {

    private volatile OutputStream os;
    protected final Layout<?> layout;
    private ByteBufferDestination byteBufferDestination;

    protected OutputStreamManager(final OutputStream os, final String streamName, final Layout<?> layout,
            final boolean writeHeader) {
        super(streamName);
        this.os = os;
        this.layout = layout;
        if (writeHeader && layout != null) {
            final byte[] header = layout.getHeader();
            if (header != null) {
                try {
                    this.os.write(header, 0, header.length);
                } catch (final IOException e) {
                    logError("unable to write header", e);
                }
            }
        }
    }

    /**
     * Creates a Manager.
     *
     * @param name The name of the stream to manage.
     * @param data The data to pass to the Manager.
     * @param factory The factory to use to create the Manager.
     * @param <T> The type of the OutputStreamManager.
     * @return An OutputStreamManager.
     */
    public static <T> OutputStreamManager getManager(final String name, final T data,
                                                 final ManagerFactory<? extends OutputStreamManager, T> factory) {
        return AbstractManager.getManager(name, factory, data);
    }

    /**
     * Default hook to write footer during close.
     */
    @Override
    public void releaseSub() {
        writeFooter();
        close();
    }

    /**
     * Writes the footer.
     */
    protected void writeFooter() {
        if (layout == null) {
            return;
        }
        final byte[] footer = layout.getFooter();
        if (footer != null) {
            write(footer);
        }
    }

    /**
     * Returns the status of the stream.
     * @return true if the stream is open, false if it is not.
     */
    public boolean isOpen() {
        return getCount() > 0;
    }

    protected OutputStream getOutputStream() {
        return os;
    }

    protected void setOutputStream(final OutputStream os) {
        final byte[] header = layout.getHeader();
        if (header != null) {
            try {
                os.write(header, 0, header.length);
                this.os = os; // only update field if os.write() succeeded
            } catch (final IOException ioe) {
                logError("unable to write header", ioe);
            }
        } else {
            this.os = os;
        }
    }

    /**
     * Some output streams synchronize writes while others do not. Synchronizing here insures that
     * log events won't be intertwined.
     * @param bytes The serialized Log event.
     * @param offset The offset into the byte array.
     * @param length The number of bytes to write.
     * @throws AppenderLoggingException if an error occurs.
     */
    protected void write(final byte[] bytes, final int offset, final int length) {
        flushBuffer();
        write(bytes, offset, length, false);
    }

    /**
     * Some output streams synchronize writes while others do not. Synchronizing here insures that
     * log events won't be intertwined.
     * @param bytes The serialized Log event.
     * @param offset The offset into the byte array.
     * @param length The number of bytes to write.
     * @param immediateFlush flushes immediately after writing.
     * @throws AppenderLoggingException if an error occurs.
     */
    protected synchronized void write(final byte[] bytes, final int offset, final int length, boolean immediateFlush) {
        // System.out.println("write " + count);
        try {
            os.write(bytes, offset, length);
            if (immediateFlush) {
                os.flush();
            }
        } catch (final IOException ex) {
            final String msg = "Error writing to stream " + getName();
            throw new AppenderLoggingException(msg, ex);
        }
    }

    /**
     * Some output streams synchronize writes while others do not.
     * @param bytes The serialized Log event.
     * @throws AppenderLoggingException if an error occurs.
     */
    protected void write(final byte[] bytes)  {
        flushBuffer();
        write(bytes, 0, bytes.length, false);
    }

    /**
     * Some output streams synchronize writes while others do not.
     * @param bytes The serialized Log event.
     * @param immediateFlush If true, flushes after writing.
     * @throws AppenderLoggingException if an error occurs.
     */
    protected void write(final byte[] bytes, boolean immediateFlush)  {
        flushBuffer();
        write(bytes, 0, bytes.length, immediateFlush);
    }

    protected synchronized void close() {
        flush();
        final OutputStream stream = os; // access volatile field only once per method
        if (stream == System.out || stream == System.err) {
            return;
        }
        try {
            stream.close();
        } catch (final IOException ex) {
            logError("unable to close stream", ex);
        }
    }

    /**
     * Flushes any buffers.
     */
    public synchronized void flush() {
        try {
            flushBuffer();
            os.flush();
        } catch (final IOException ex) {
            final String msg = "Error flushing stream " + getName();
            throw new AppenderLoggingException(msg, ex);
        }
    }

    /**
     * Drains the ByteBufferDestination's buffer into the destination. By default this calls
     * {@link OutputStreamManager#write(byte[], int, int, boolean)} with the buffer contents and the Appender's
     * {@link AbstractOutputStreamAppender#immediateFlush} value.
     * <p>
     * This method has no effect if the garbage-free Layout encode mechanism is not enabled.
     * </p>
     */
    protected void flushBuffer() {
        if (Constants.ENABLE_DIRECT_ENCODERS) {
            final ByteBufferDestination destination = getByteBufferDestination();
            synchronized (destination) {
                destination.drain(destination.getByteBuffer());
            }
        }
    }

    /**
     * Subclasses that do buffered IO should override.
     * @return this implementation always returns {@code false}
     */
    protected boolean isBufferedIO() {
        return false;
    }

    public ByteBufferDestination getByteBufferDestination() {
        return byteBufferDestination;
    }

    public void setByteBufferDestination(final ByteBufferDestination byteBufferDestination) {
        this.byteBufferDestination = byteBufferDestination;
    }

    /**
     * When the garbage-free Layout.encode mechanism is used, this method is called to create a ByteBufferDestination
     * for this OutputStreamManager.
     *
     * @param immediateFlush the value to pass to the {@link #write(byte[], int, int, boolean)} method when the
     *          ByteBufferDestination is {@link ByteBufferDestination#drain(ByteBuffer) drained}
     * @return a new ByteBufferDestination that drains into this OutputStreamManager
     */
    protected ByteBufferDestination createByteBufferDestination(final boolean immediateFlush) {
        return new OutputStreamManagerDestination(immediateFlush, this);
    }
}
