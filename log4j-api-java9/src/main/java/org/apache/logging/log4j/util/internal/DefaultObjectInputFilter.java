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
package org.apache.logging.log4j.util.internal;

import java.io.ObjectInputFilter;
import java.util.Arrays;
import java.util.List;

public class DefaultObjectInputFilter implements ObjectInputFilter {


    private static final List<String> REQUIRED_JAVA_CLASSES = Arrays.asList(
            "java.math.BigDecimal",
            "java.math.BigInteger",
            // for Message delegate
            "java.rmi.MarshalledObject",
            "[B"
    );

    private static final List<String> REQUIRED_JAVA_PACKAGES = Arrays.asList(
            "java.lang.",
            "java.time",
            "java.util.",
            "org.apache.logging.log4j.",
            "[Lorg.apache.logging.log4j."
    );

    private final ObjectInputFilter delegate;

    public DefaultObjectInputFilter() {
        delegate = null;
    }

    public DefaultObjectInputFilter(ObjectInputFilter filter) {
        delegate = filter;
    }

    /**
     * This is easier to use via reflection.
     * @param filter The ObjectInputFilter.
     * @return The DefaultObjectInputFilter.
     */
    public static DefaultObjectInputFilter newInstance(ObjectInputFilter filter) {
        return new DefaultObjectInputFilter(filter);
    }


    @Override
    public Status checkInput(FilterInfo filterInfo) {
        Status status = null;
        if (delegate != null) {
            status = delegate.checkInput(filterInfo);
            if (status != Status.UNDECIDED) {
                return status;
            }
        }
        ObjectInputFilter serialFilter = ObjectInputFilter.Config.getSerialFilter();
        if (serialFilter != null) {
            status = serialFilter.checkInput(filterInfo);
            if (status != Status.UNDECIDED) {
                // The process-wide filter overrides this filter
                return status;
            }
        }
        if (filterInfo.serialClass() != null) {
            String name = filterInfo.serialClass().getName();
            if (isAllowedByDefault(name) || isRequiredPackage(name)) {
                return Status.ALLOWED;
            }
        }
        return Status.REJECTED;
    }

    private static boolean isAllowedByDefault(final String name) {
        return isRequiredPackage(name) || REQUIRED_JAVA_CLASSES.contains(name);
    }

    private static boolean isRequiredPackage(final String name) {
        for (final String packageName : REQUIRED_JAVA_PACKAGES) {
            if (name.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }
}
