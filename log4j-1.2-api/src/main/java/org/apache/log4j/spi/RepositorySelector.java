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
package org.apache.log4j.spi;

/**

 The <code>LogManager</code> uses one (and only one)
 <code>RepositorySelector</code> implementation to select the
 {@link org.apache.log4j.spi.LoggerRepository} for a particular application context.

 <p>It is the responsibility of the <code>RepositorySelector</code>
 implementation to track the application context. Log4j makes no
 assumptions about the application context or on its management.

 <p>See also {@link org.apache.log4j.LogManager LogManager}.

 @since 1.2

 */
public interface RepositorySelector {

    /**
     * Returns a {@link org.apache.log4j.spi.LoggerRepository} depending on the
     * context. Implementers must make sure that a valid (non-null)
     * LoggerRepository is returned.
     * @return a LoggerRepository.
     */
    LoggerRepository getLoggerRepository();
}
