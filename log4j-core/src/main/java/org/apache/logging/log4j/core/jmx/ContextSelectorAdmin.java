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
package org.apache.logging.log4j.core.jmx;

import java.util.Objects;
import javax.management.ObjectName;
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
     * @param contextName name of the LoggerContext under which to register this
     *            ContextSelectorAdmin. Note that the ContextSelector may be
     *            registered multiple times, once for each LoggerContext. In web
     *            containers, each web application has its own LoggerContext and
     *            by associating the ContextSelector with the LoggerContext, all
     *            associated MBeans can be unloaded when the web application is
     *            undeployed.
     * @param selector the instrumented object
     */
    public ContextSelectorAdmin(final String contextName, final ContextSelector selector) {
        this.selector = Objects.requireNonNull(selector, "ContextSelector");
        try {
            final String mbeanName = String.format(PATTERN, Server.escape(contextName));
            objectName = new ObjectName(mbeanName);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the {@code ObjectName} of this mbean.
     *
     * @return the {@code ObjectName}
     * @see ContextSelectorAdminMBean#PATTERN
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public String getImplementationClassName() {
        return selector.getClass().getName();
    }
}
