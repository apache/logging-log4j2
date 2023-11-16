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
package org.apache.logging.log4j.core.layout;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import java.util.HashSet;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.JsonConstants;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.apache.logging.log4j.core.jackson.Log4jXmlObjectMapper;
import org.apache.logging.log4j.core.jackson.Log4jYamlObjectMapper;
import org.apache.logging.log4j.core.jackson.XmlConstants;
import org.codehaus.stax2.XMLStreamWriter2;

abstract class JacksonFactory {

    static class JSON extends JacksonFactory {

        private final boolean encodeThreadContextAsList;
        private final boolean includeStacktrace;
        private final boolean stacktraceAsString;
        private final boolean objectMessageAsJsonObject;

        public JSON(
                final boolean encodeThreadContextAsList,
                final boolean includeStacktrace,
                final boolean stacktraceAsString,
                final boolean objectMessageAsJsonObject) {
            this.encodeThreadContextAsList = encodeThreadContextAsList;
            this.includeStacktrace = includeStacktrace;
            this.stacktraceAsString = stacktraceAsString;
            this.objectMessageAsJsonObject = objectMessageAsJsonObject;
        }

        @Override
        protected String getPropertNameForContextMap() {
            return JsonConstants.ELT_CONTEXT_MAP;
        }

        @Override
        protected String getPropertyNameForTimeMillis() {
            return JsonConstants.ELT_TIME_MILLIS;
        }

        @Override
        protected String getPropertyNameForInstant() {
            return JsonConstants.ELT_INSTANT;
        }

        @Override
        protected String getPropertNameForSource() {
            return JsonConstants.ELT_SOURCE;
        }

        @Override
        protected String getPropertNameForNanoTime() {
            return JsonConstants.ELT_NANO_TIME;
        }

        @Override
        protected PrettyPrinter newCompactPrinter() {
            return new MinimalPrettyPrinter();
        }

        @Override
        protected ObjectMapper newObjectMapper() {
            return new Log4jJsonObjectMapper(
                    encodeThreadContextAsList, includeStacktrace, stacktraceAsString, objectMessageAsJsonObject);
        }

        @Override
        protected PrettyPrinter newPrettyPrinter() {
            return new DefaultPrettyPrinter();
        }
    }

    static class XML extends JacksonFactory {

        static final int DEFAULT_INDENT = 1;

        private final boolean includeStacktrace;
        private final boolean stacktraceAsString;

        public XML(final boolean includeStacktrace, final boolean stacktraceAsString) {
            this.includeStacktrace = includeStacktrace;
            this.stacktraceAsString = stacktraceAsString;
        }

        @Override
        protected String getPropertyNameForTimeMillis() {
            return XmlConstants.ELT_TIME_MILLIS;
        }

        @Override
        protected String getPropertyNameForInstant() {
            return XmlConstants.ELT_INSTANT;
        }

        @Override
        protected String getPropertNameForContextMap() {
            return XmlConstants.ELT_CONTEXT_MAP;
        }

        @Override
        protected String getPropertNameForSource() {
            return XmlConstants.ELT_SOURCE;
        }

        @Override
        protected String getPropertNameForNanoTime() {
            return JsonConstants.ELT_NANO_TIME;
        }

        @Override
        protected PrettyPrinter newCompactPrinter() {
            // Yes, null is the proper answer.
            return null;
        }

        @Override
        protected ObjectMapper newObjectMapper() {
            return new Log4jXmlObjectMapper(includeStacktrace, stacktraceAsString);
        }

        @Override
        protected PrettyPrinter newPrettyPrinter() {
            return new Log4jXmlPrettyPrinter(DEFAULT_INDENT);
        }
    }

    static class YAML extends JacksonFactory {

        private final boolean includeStacktrace;
        private final boolean stacktraceAsString;

        public YAML(final boolean includeStacktrace, final boolean stacktraceAsString) {
            this.includeStacktrace = includeStacktrace;
            this.stacktraceAsString = stacktraceAsString;
        }

        @Override
        protected String getPropertyNameForTimeMillis() {
            return JsonConstants.ELT_TIME_MILLIS;
        }

        @Override
        protected String getPropertyNameForInstant() {
            return JsonConstants.ELT_INSTANT;
        }

        @Override
        protected String getPropertNameForContextMap() {
            return JsonConstants.ELT_CONTEXT_MAP;
        }

        @Override
        protected String getPropertNameForSource() {
            return JsonConstants.ELT_SOURCE;
        }

        @Override
        protected String getPropertNameForNanoTime() {
            return JsonConstants.ELT_NANO_TIME;
        }

        @Override
        protected PrettyPrinter newCompactPrinter() {
            return new MinimalPrettyPrinter();
        }

        @Override
        protected ObjectMapper newObjectMapper() {
            return new Log4jYamlObjectMapper(false, includeStacktrace, stacktraceAsString);
        }

        @Override
        protected PrettyPrinter newPrettyPrinter() {
            return new DefaultPrettyPrinter();
        }
    }

    /**
     * When &lt;Event&gt;s are written into a XML file; the "Event" object is not the root element, but an element named
     * &lt;Events&gt; created using {@link XmlLayout#getHeader()} and {@link XmlLayout#getFooter()} methods.
     * <p>
     * {@link com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter} is used to print the Event object into
     * XML; hence it assumes &lt;Event&gt; tag as the root element, so it prints the &lt;Event&gt; tag without any
     * indentation. To add an indentation to the &lt;Event&gt; tag; hence an additional indentation for any
     * sub-elements, this class is written. As an additional task, to avoid the blank line printed after the ending
     * &lt;/Event&gt; tag, {@link #writePrologLinefeed(XMLStreamWriter2)} method is also overridden.
     * </p>
     */
    static class Log4jXmlPrettyPrinter extends DefaultXmlPrettyPrinter {

        private static final long serialVersionUID = 1L;

        Log4jXmlPrettyPrinter(final int nesting) {
            _nesting = nesting;
        }

        @Override
        public void writePrologLinefeed(final XMLStreamWriter2 sw) throws XMLStreamException {
            // nothing
        }

        /**
         * Sets the nesting level to 1 rather than 0, so the "Event" tag will get indentation of next level below root.
         */
        @Override
        public DefaultXmlPrettyPrinter createInstance() {
            return new Log4jXmlPrettyPrinter(XML.DEFAULT_INDENT);
        }
    }

    protected abstract String getPropertyNameForTimeMillis();

    protected abstract String getPropertyNameForInstant();

    protected abstract String getPropertNameForContextMap();

    protected abstract String getPropertNameForSource();

    protected abstract String getPropertNameForNanoTime();

    protected abstract PrettyPrinter newCompactPrinter();

    protected abstract ObjectMapper newObjectMapper();

    protected abstract PrettyPrinter newPrettyPrinter();

    ObjectWriter newWriter(final boolean locationInfo, final boolean properties, final boolean compact) {
        return newWriter(locationInfo, properties, compact, false);
    }

    ObjectWriter newWriter(
            final boolean locationInfo, final boolean properties, final boolean compact, final boolean includeMillis) {
        final SimpleFilterProvider filters = new SimpleFilterProvider();
        final Set<String> except = new HashSet<>(3);
        if (!locationInfo) {
            except.add(this.getPropertNameForSource());
        }
        if (!properties) {
            except.add(this.getPropertNameForContextMap());
        }
        if (includeMillis) {
            except.add(getPropertyNameForInstant());
        } else {
            except.add(getPropertyNameForTimeMillis());
        }
        except.add(this.getPropertNameForNanoTime());
        filters.addFilter(Log4jLogEvent.class.getName(), SimpleBeanPropertyFilter.serializeAllExcept(except));
        final ObjectWriter writer =
                this.newObjectMapper().writer(compact ? this.newCompactPrinter() : this.newPrettyPrinter());
        return writer.with(filters);
    }
}
