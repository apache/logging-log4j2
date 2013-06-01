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

/**
 * @doubt unchecked exception again (RG) Why is that a problem? A runtime exception
 * is appropriate in the case where the Appender encounters a checked exception and
 * needs to percolate the exception to the application.
 */
public class AppenderRuntimeException extends RuntimeException {

    /**
     * Generated serial version ID.
     */
    private static final long serialVersionUID = 6545990597472958303L;

    public AppenderRuntimeException(final String msg) {
        super(msg);
    }

    public AppenderRuntimeException(final String msg, final Throwable ex) {
        super(msg, ex);
    }

    public AppenderRuntimeException(final Throwable ex) {
        super(ex);
    }
}
