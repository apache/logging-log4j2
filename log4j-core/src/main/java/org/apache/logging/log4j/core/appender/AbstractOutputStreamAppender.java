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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;

/**
 * Appends log events as bytes to a byte output stream. The stream encoding is defined in the layout.
 *
 * @param <M> The kind of {@link OutputStreamManager} under management
 */
public abstract class AbstractOutputStreamAppender<M extends OutputStreamManager> extends
        AbstractByteBufferDestinationAppender<M> {

    /**
     * Instantiates a WriterAppender and set the output destination to a new {@link java.io.OutputStreamWriter}
     * initialized with <code>os</code> as its {@link java.io.OutputStream}.
     *
     * @param name The name of the Appender.
     * @param layout The layout to format the message.
     * @param manager The OutputStreamManager.
     */
    protected AbstractOutputStreamAppender(final String name, final Layout<? extends Serializable> layout,
            final Filter filter, final boolean ignoreExceptions, final boolean immediateFlush, final M manager) {
        super(name, layout, filter, ignoreExceptions, immediateFlush, manager);
    }

}
