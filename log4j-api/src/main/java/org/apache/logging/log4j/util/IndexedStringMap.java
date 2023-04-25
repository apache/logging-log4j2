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

/**
 * An extension of {@code StringMap} that imposes a total ordering on its keys.
 * The map is ordered according to the natural ordering of its keys. This order is reflected when
 * {@link #forEach(BiConsumer) consuming} the key-value pairs with a {@link BiConsumer} or a {@link TriConsumer}.
 * <p>
 * This interface views all key-value pairs as a sequence ordered by key, and allows
 * keys and values to be accessed by their index in the sequence.
 * </p>
 *
 * @see IndexedReadOnlyStringMap
 * @see StringMap
 * @since 2.8
 */
public interface IndexedStringMap extends IndexedReadOnlyStringMap, StringMap {
    // nothing more
}
