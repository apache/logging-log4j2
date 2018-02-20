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
package org.apache.logging.log4j.jackson;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.core.impl.Log4jLogEvent;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public abstract class AbstractJacksonFactory {

    protected final boolean includeStacktrace;

    protected final boolean stacktraceAsString;
    public AbstractJacksonFactory(final boolean includeStacktrace, final boolean stacktraceAsString) {
        super();
        this.includeStacktrace = includeStacktrace;
        this.stacktraceAsString = stacktraceAsString;
    }

    abstract protected String getPropertyNameForContextMap();

    abstract protected String getPropertyNameForNanoTime();

    abstract protected String getPropertyNameForSource();

    abstract protected String getPropertyNameForStackTrace();

    abstract protected PrettyPrinter newCompactPrinter();

    abstract protected ObjectMapper newObjectMapper();

    abstract protected PrettyPrinter newPrettyPrinter();

    public ObjectWriter newWriter(final boolean locationInfo, final boolean properties, final boolean compact) {
        final SimpleFilterProvider filters = new SimpleFilterProvider();
        final Set<String> except = new HashSet<>(4);
        if (!locationInfo) {
            except.add(this.getPropertyNameForSource());
        }
        if (!properties) {
            except.add(this.getPropertyNameForContextMap());
        }
        if (!includeStacktrace) {
            except.add(this.getPropertyNameForStackTrace());
        }
        except.add(this.getPropertyNameForNanoTime());
        filters.addFilter(Log4jLogEvent.class.getName(), SimpleBeanPropertyFilter.serializeAllExcept(except));
        final ObjectWriter writer = this.newObjectMapper()
                .writer(compact ? this.newCompactPrinter() : this.newPrettyPrinter());
        return writer.with(filters);
    }

}
