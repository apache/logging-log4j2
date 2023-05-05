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

/**
 * The MBean interface for monitoring and managing the {@code ContextSelector}.
 */
public interface ContextSelectorAdminMBean {
    /**
     * ObjectName pattern ({@value}) for ContextSelectorAdmin MBeans.
     * This pattern contains a variable, which is the name of the logger context.
     * <p>
     * You can find all registered ContextSelectorAdmin MBeans like this:
     * </p>
     * <pre>
     * MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
     * String pattern = String.format(ContextSelectorAdminMBean.PATTERN, &quot;*&quot;);
     * Set&lt;ObjectName&gt; contextSelectorNames = mbs.queryNames(new ObjectName(pattern), null);
     * </pre>
     * <p>
     * Some characters are not allowed in ObjectNames. The logger context name
     * may be quoted. When ContextSelectorAdmin MBeans are
     * registered, their ObjectNames are created using this pattern as follows:
     * </p>
     * <pre>
     * String ctxName = Server.escape(loggerContext.getName());
     * String name = String.format(PATTERN, ctxName);
     * ObjectName objectName = new ObjectName(name);
     * </pre>
     * @see Server#escape(String)
     */
    String PATTERN = Server.DOMAIN + ":type=%s,component=ContextSelector";

    /**
     * Returns the name of the class implementing the {@code ContextSelector}
     * interface.
     *
     * @return the name of the {@code ContextSelector} implementation class.
     */
    String getImplementationClassName();
}
