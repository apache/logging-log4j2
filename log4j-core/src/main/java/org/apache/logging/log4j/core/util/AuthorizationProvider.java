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
package org.apache.logging.log4j.core.util;

import java.net.URLConnection;
import org.apache.logging.log4j.core.impl.CoreProperties.AuthenticationProperties;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Interface to be implemented to add an Authorization header to an HTTP request.
 */
@FunctionalInterface
public interface AuthorizationProvider {

    void addAuthorization(URLConnection urlConnection);

    static AuthorizationProvider getAuthorizationProvider(final AuthenticationProperties props) {
        if (props.type() != null) {
            try {
                return LoaderUtil.newInstanceOf(props.type());
            } catch (final ReflectiveOperationException | LinkageError e) {
                StatusLogger.getLogger()
                        .warn("Unable to create {}, using default", props.type().getName(), e);
            }
        }
        return new BasicAuthorizationProvider(props.basic());
    }
}
