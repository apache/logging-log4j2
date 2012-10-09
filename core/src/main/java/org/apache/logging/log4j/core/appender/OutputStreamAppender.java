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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Writes the byte output stream. The stream will already have been encoded.
 */
public abstract class OutputStreamAppender extends AbstractAppender {

    /**
     * Immediate flush means that the underlying writer or output stream
     * will be flushed at the end of each append operation. Immediate
     * flush is slower but ensures that each append request is actually
     * written. If <code>immediateFlush</code> is set to
     * <code>false</code>, then there is a good chance that the last few
     * logs events are not actually written to persistent media if and
     * when the application crashes.
     * <p/>
     * <p>The <code>immediateFlush</code> variable is set to
     * <code>true</code> by default.
     */
    protected boolean immediateFlush = true;

    private volatile OutputStreamManager manager;

    private ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private Lock readLock = rwLock.readLock();
    private Lock writeLock = rwLock.writeLock();

    /**
     * Instantiate a WriterAppender and set the output destination to a
     * new {@link java.io.OutputStreamWriter} initialized with <code>os</code>
     * as its {@link java.io.OutputStream}.
     * @param name The name of the Appender.
     * @param layout The layout to format the message.
     * @param manager The OutputStreamManager.
     */
    protected OutputStreamAppender(String name, Layout layout, Filter filter, boolean handleException,
                                boolean immediateFlush, OutputStreamManager manager) {
        super(name, filter, layout, handleException);
        if (layout != null) {
            manager.setHeader(layout.getHeader());
            manager.setFooter(layout.getFooter());
        }
        this.manager = manager;
        this.immediateFlush = immediateFlush;
    }

    protected OutputStreamManager getManager() {
        return manager;
    }

    protected void replaceManager(OutputStreamManager newManager) {

        writeLock.lock();
        try {
            OutputStreamManager old = manager;
            manager = newManager;
            old.release();
        } finally {
            writeLock.unlock();
        }

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
    public void stop() {
        super.stop();
        manager.release();
    }

    /**
     * Actual writing occurs here.
     * <p/>
     * <p>Most subclasses of <code>OutputStreamAppender</code> will need to
     * override this method.
     * @param event The LogEvent.
     */
    public void append(LogEvent event) {
        readLock.lock();
        try {
            manager.write(getLayout().format(event));
            if (this.immediateFlush) {
                manager.flush();
            }
        } catch (AppenderRuntimeException ex) {
            error("Unable to write to stream " + manager.getName() + " for appender " + getName());
            throw ex;
        } finally {
            readLock.unlock();
        }
    }
}
