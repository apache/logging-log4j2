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
package org.apache.logging.log4j.core.jmx;

import javax.management.ObjectName;

import org.apache.logging.log4j.core.helpers.Assert;
import org.apache.logging.log4j.core.selector.ContextSelector;

/**
 * Implementation of the {@code ContextSelectorAdminMBean} interface.
 */
public class ContextSelectorAdmin implements ContextSelectorAdminMBean {

    private final ObjectName objectName;
    private final ContextSelector selector;

    /**
     * Constructs a new {@code ContextSelectorAdmin}.
     *
     * @param selector the instrumented object
     */
    public ContextSelectorAdmin(final ContextSelector selector) {
        super();
        this.selector = Assert.isNotNull(selector, "ContextSelector");
        try {
            objectName = new ObjectName(NAME);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the {@code ObjectName} of this mbean.
     *
     * @return the {@code ObjectName}
     * @see ContextSelectorAdminMBean#NAME
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public String getImplementationClassName() {
        return selector.getClass().getName();
    }
}
