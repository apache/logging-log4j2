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

package org.apache.logging.log4j.core.config.di;

import org.apache.logging.log4j.plugins.di.Disposes;
import org.apache.logging.log4j.plugins.di.Inject;
import org.apache.logging.log4j.plugins.di.Produces;

import java.util.Collection;

/**
 * Provides lifecycle operations for producing instances of a specified type. Producers represent three different ways
 * to manage instances: injectable classes, producer methods, and producer fields.
 *
 * @param <T> type of instance managed by this producer
 * @see Inject
 * @see Produces
 */
public interface Producer<T> {

    /**
     * Creates an instance in the given initialization context. When this represents a class, this calls the
     * constructor annotated with {@link Inject} or the default constructor.
     * When this represents a producer method, this invokes the producer method. When this represents a producer
     * field, this obtains the value of the field.
     *
     * @param context initialization context to use to create the instance
     * @return produced instance
     * @see Produces
     */
    T produce(InitializationContext<T> context);

    /**
     * Destroys the provided instance. When this represents a class, this does nothing. When this represents a
     * producer method or field, then this invokes the corresponding disposer method.
     *
     * @param instance instance to dispose
     * @see Disposes
     */
    void dispose(T instance);

    /**
     * Returns all the injection points of this producer. For a class, this returns all injected fields, bean
     * constructor parameters, and initialization method parameters. For a producer method, this returns the
     * parameters to that method.
     *
     * @return all injection points for this producer
     * @see Inject
     * @see Produces
     */
    Collection<InjectionPoint> getInjectionPoints();
}
