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

/**
 * Manage an OutputStream so that it can be shared by multiple Appenders and will
 * allow appenders to reconfigure without requiring a new stream.
 */
public class OutputStreamManager extends AbstractManager {

    private OutputStream os;

    private byte[] footer = null;
    private byte[] header = null;

    protected OutputStreamManager(final OutputStream os, final String streamName) {
        super(streamName);
        this.os = os;
    }

    /**
     * Create a Manager.
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
     * Set the header to write when the stream is opened.
     * @param header The header.
     */
    public synchronized void setHeader(final byte[] header) {
        if (header != null) {
            this.header = header;
            try {
                this.os.write(header, 0, header.length);
            } catch (final IOException ioe) {
                LOGGER.error("Unable to write header", ioe);
            }
        }
    }

    /**
     * Set the footer to write when the stream is closed.
     * @param footer The footer.
     */
    public synchronized void setFooter(final byte[] footer) {
        if (footer != null) {
            this.footer = footer;
        }
    }

    /**
     * Default hook to write footer during close.
     */
    @Override
    public void releaseSub() {
        if (footer != null) {
            write(footer);
        }
        close();
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
        this.os = os;
        if (header != null) {
            try {
                this.os.write(header, 0, header.length);
            } catch (final IOException ioe) {
                LOGGER.error("Unable to write header", ioe);
            }
        }
    }

    /**
     * Some output streams synchronize writes while others do not. Synchronizing here insures that
     * log events won't be intertwined.
     * @param bytes The serialized Log event.
     * @param offset The offset into the byte array.
     * @param length The number of bytes to write.
     * @throws AppenderRuntimeException if an error occurs.
     */
    protected synchronized void write(final byte[] bytes, final int offset, final int length)  {
        //System.out.println("write " + count);
        try {
            os.write(bytes, offset, length);
        } catch (final IOException ex) {
            final String msg = "Error writing to stream " + getName();
            throw new AppenderRuntimeException(msg, ex);
        }
    }

    /**
     * Some output streams synchronize writes while others do not. Synchronizing here insures that
     * log events won't be intertwined.
     * @param bytes The serialized Log event.
     * @throws AppenderRuntimeException if an error occurs.
     */
    protected void write(final byte[] bytes)  {
        write(bytes, 0, bytes.length);
    }

    protected void close() {
        if (os == System.out || os == System.err) {
            return;
        }
        try {
            os.close();
        } catch (final IOException ex) {
            LOGGER.error("Unable to close stream " + getName() + ". " + ex);
        }
    }

    /**
     * Flush any buffers.
     */
    public void flush() {
        try {
            os.flush();
        } catch (final IOException ex) {
            final String msg = "Error flushing stream " + getName();
            throw new AppenderRuntimeException(msg, ex);
        }
    }
}
