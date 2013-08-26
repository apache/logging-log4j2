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
package org.apache.logging.log4j.taglib;

import org.apache.logging.log4j.Level;

/**
 * This class implements the {@code <log:log>} tag.
 *
 * @since 2.0
 */
public class LogTag extends LoggingMessageTagSupport {
    private static final long serialVersionUID = 1L;

    private Level level;

    @Override
    protected void init() {
        super.init();
        this.level = null;
    }

    @Override
    protected Level getLevel() {
        return this.level;
    }

    public void setLevel(final Object level) {
        this.level = TagUtils.resolveLevel(level);
    }
}
