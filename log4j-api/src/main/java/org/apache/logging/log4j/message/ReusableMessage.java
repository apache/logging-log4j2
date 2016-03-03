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
package org.apache.logging.log4j.message;

import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * Messages implementing this interface are reused.
 * <p>
 * If a Message is reusable, downstream components should not hand over this instance to another thread, but extract its
 * content (via the {@link StringBuilderFormattable#formatTo(StringBuilder)} method) instead.
 * </p>
 */
public interface ReusableMessage extends Message, StringBuilderFormattable {
}
