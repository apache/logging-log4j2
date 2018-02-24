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
// Lines too long...
//CHECKSTYLE:OFF
/**
 * The converters in this package implement the JPA 2.1 mechanism for converting non-standard types to and from
 * database fields. Most of these types are capable of two-way conversion and can be used to both persist and retrieve
 * entities. The
 * {@link org.apache.logging.log4j.core.appender.db.jpa.converter.ContextMapAttributeConverter ContextMapAttributeConverter}
 * and {@link org.apache.logging.log4j.core.appender.db.jpa.converter.ContextStackAttributeConverter ContextStackAttributeConverter}
 * only support persistence and not retrieval, persisting the type as a simple string. You can use the
 * {@link org.apache.logging.log4j.core.appender.db.jpa.converter.ContextMapJsonAttributeConverter ContextMapJsonAttributeConverter}
 * and {@link org.apache.logging.log4j.core.appender.db.jpa.converter.ContextStackJsonAttributeConverter ContextStackJsonAttributeConverter}
 * instead, which require the Jackson Data Processor dependency to also be on your class path.
 */
//CHECKSTYLE:ON
package org.apache.logging.log4j.core.appender.db.jpa.converter;

