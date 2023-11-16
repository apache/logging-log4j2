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
package org.apache.logging.log4j.util.internal;

import static org.apache.logging.log4j.util.internal.SerializationUtil.REQUIRED_JAVA_CLASSES;
import static org.apache.logging.log4j.util.internal.SerializationUtil.REQUIRED_JAVA_PACKAGES;

import java.io.ObjectInputFilter;

public class DefaultObjectInputFilter implements ObjectInputFilter {

    private final ObjectInputFilter delegate;

    public DefaultObjectInputFilter() {
        delegate = null;
    }

    public DefaultObjectInputFilter(final ObjectInputFilter filter) {
        delegate = filter;
    }

    /**
     * This is easier to use via reflection.
     * @param filter The ObjectInputFilter.
     * @return The DefaultObjectInputFilter.
     */
    public static DefaultObjectInputFilter newInstance(final ObjectInputFilter filter) {
        return new DefaultObjectInputFilter(filter);
    }

    @Override
    public Status checkInput(final FilterInfo filterInfo) {
        Status status = null;
        if (delegate != null) {
            status = delegate.checkInput(filterInfo);
            if (status != Status.UNDECIDED) {
                return status;
            }
        }
        final ObjectInputFilter serialFilter = ObjectInputFilter.Config.getSerialFilter();
        if (serialFilter != null) {
            status = serialFilter.checkInput(filterInfo);
            if (status != Status.UNDECIDED) {
                // The process-wide filter overrides this filter
                return status;
            }
        }
        if (filterInfo.serialClass() != null) {
            final String name = filterInfo.serialClass().getName();
            if (isAllowedByDefault(name) || isRequiredPackage(name)) {
                return Status.ALLOWED;
            }
        } else {
            // Object already deserialized
            return Status.ALLOWED;
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
