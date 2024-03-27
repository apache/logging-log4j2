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
package org.apache.logging.log4j.util;

import org.apache.logging.log4j.message.Message;

/**
 * Classes implementing this interface know how to supply {@link Message}s.
 *
 * <p>This is a <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">functional
 * interface</a> intended to support lambda expressions in log4j 2.
 *
 * <p>Implementors are free to cache values or return a new or distinct value each time the supplier is invoked.
 *
 * <p><strong>DEPRECATED:</strong> this class should not normally be used outside a Java 8+ lambda syntax. Instead,
 * {@link Supplier Supplier<Message>} should be used as an anonymous class. Both this and {@link Supplier} will be
 * removed in 3.0.
 * </p>
 *
 * @since 2.4
 */
@FunctionalInterface
public interface MessageSupplier extends Supplier<Message> {

    /**
     * Gets a Message.
     *
     * @return a Message
     */
    @Override
    Message get();
}
