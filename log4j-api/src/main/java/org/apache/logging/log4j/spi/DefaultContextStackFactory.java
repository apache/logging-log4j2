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

package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util3.PropertiesUtil;

import static org.apache.logging.log4j.spi.DefaultContextMapFactory.DISABLE_ALL;

public class DefaultContextStackFactory implements ThreadContextStack.Factory {
    static final String DISABLE_STACK = "disableThreadContextStack";
    private final boolean stackEnabled;

    public DefaultContextStackFactory() {
        this(PropertiesUtil.getProperties());
    }

    public DefaultContextStackFactory(final PropertyEnvironment properties) {
        this(!(properties.getBooleanProperty(DISABLE_ALL) || properties.getBooleanProperty(DISABLE_STACK)));
    }

    public DefaultContextStackFactory(final boolean stackEnabled) {
        this.stackEnabled = stackEnabled;
    }

    @Override
    public ThreadContextStack createThreadContextStack() {
        return new DefaultThreadContextStack(stackEnabled);
    }

    @Override
    public boolean isEnabled() {
        return stackEnabled;
    }
}
