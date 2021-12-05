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
package org.apache.log4j.builders;

import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.Filter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Base class for Log4j 1 component builders.
 */
public abstract class AbstractBuilder {

    private static Logger LOGGER = StatusLogger.getLogger();
    protected static final String FILE_PARAM = "File";
    protected static final String APPEND_PARAM = "Append";
    protected static final String BUFFERED_IO_PARAM = "BufferedIO";
    protected static final String BUFFER_SIZE_PARAM = "BufferSize";
    protected static final String MAX_SIZE_PARAM = "MaxFileSize";
    protected static final String MAX_BACKUP_INDEX = "MaxBackupIndex";
    protected static final String RELATIVE = "RELATIVE";

    private final String prefix;
    private final Properties props;
    private final StrSubstitutor strSubstitutor;

    public AbstractBuilder() {
        this.prefix = null;
        this.props = new Properties();
        strSubstitutor = new StrSubstitutor(System.getProperties());
    }

    public AbstractBuilder(String prefix, Properties props) {
        this.prefix = prefix + ".";
        this.props = props;
        Map<String, String> map = new HashMap<>();
        System.getProperties().forEach((k, v) -> map.put(k.toString(), v.toString()));
        props.forEach((k, v) -> map.put(k.toString(), v.toString()));
        strSubstitutor = new StrSubstitutor(map);
    }

    public String getProperty(String key) {
        return strSubstitutor.replace(props.getProperty(prefix + key));
    }

    public String getProperty(String key, String defaultValue) {
        return strSubstitutor.replace(props.getProperty(prefix + key, defaultValue));
    }

    public boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(strSubstitutor.replace(props.getProperty(prefix + key, Boolean.FALSE.toString())));
    }

    public int getIntegerProperty(String key, int defaultValue) {
        String value = getProperty(key);
        try {
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (Exception ex) {
            LOGGER.warn("Error converting value {} of {} to an integer: {}", value, key, ex.getMessage());
        }
        return defaultValue;
    }

    public Properties getProperties() {
        return props;
    }


    protected org.apache.logging.log4j.core.Filter buildFilters(String level, Filter filter) {
        if (level != null && filter != null) {
            List<org.apache.logging.log4j.core.Filter> filterList = new ArrayList<>();
            org.apache.logging.log4j.core.Filter thresholdFilter =
                    ThresholdFilter.createFilter(OptionConverter.convertLevel(level, Level.TRACE),
                            org.apache.logging.log4j.core.Filter.Result.NEUTRAL,
                            org.apache.logging.log4j.core.Filter.Result.DENY);
            filterList.add(thresholdFilter);
            Filter f = filter;
            while (f != null) {
                if (filter instanceof FilterWrapper) {
                    filterList.add(((FilterWrapper) f).getFilter());
                } else {
                    filterList.add(new FilterAdapter(f));
                }
                f = f.next;
            }
            return CompositeFilter.createFilters(filterList.toArray(new org.apache.logging.log4j.core.Filter[0]));
        } else if (level != null) {
            return ThresholdFilter.createFilter(OptionConverter.convertLevel(level, Level.TRACE),
                    org.apache.logging.log4j.core.Filter.Result.NEUTRAL,
                    org.apache.logging.log4j.core.Filter.Result.DENY);
        } else if (filter != null) {
            if (filter instanceof FilterWrapper) {
                return ((FilterWrapper) filter).getFilter();
            }
            return new FilterAdapter(filter);
        }
        return null;
    }
}
