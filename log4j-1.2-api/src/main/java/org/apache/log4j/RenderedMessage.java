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
package org.apache.log4j;

import org.apache.log4j.or.ObjectRenderer;
import org.apache.logging.log4j.message.Message;

/**
 * Implements object rendering for Log4j 1.x compatibility.
 */
public class RenderedMessage implements Message {

    private final ObjectRenderer renderer;
    private final Object object;
    private String rendered = null;

    public RenderedMessage(final ObjectRenderer renderer, final Object object) {
        this.renderer = renderer;
        this.object = object;
    }

    @Override
    public String getFormattedMessage() {
        if (rendered == null) {
            rendered = renderer.doRender(object);
        }

        return rendered;
    }

    @Override
    public String getFormat() {
        return getFormattedMessage();
    }

    @Override
    public Object[] getParameters() {
        return null;
    }

    @Override
    public Throwable getThrowable() {
        return null;
    }
}
