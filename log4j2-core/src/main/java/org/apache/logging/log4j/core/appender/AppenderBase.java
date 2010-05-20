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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.internal.StatusLogger;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public abstract class AppenderBase implements Appender {

    protected boolean started = false;

    protected Layout layout = null;

    protected List<Filter> filters = new CopyOnWriteArrayList<Filter>();

    private final String name;

    protected static final Logger logger = StatusLogger.getLogger();

    protected ErrorHandler handler;

    public static final String NAME = "name";    

    public AppenderBase(String name, Layout layout) {
        this.name = name;
        this.layout = layout;
    }

    public ErrorHandler getHandler() {
        return handler;
    }

    public void setHandler(ErrorHandler handler) {
        this.handler = handler;
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    public Filter getFilter() {
        return filters.size() > 0 ? filters.get(0) : null;
    }

    public void clearFilters() {
        filters.clear();
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void close() {

    }

    public String getName() {
        return name;
    }

    public void setLayout(Layout layout) {
        if (layout == null) {
            handler.error("The layout for appender " + getName() + " cannot be set to null");
        }
        this.layout = layout;
    }

    public Layout getLayout() {
        return layout;
    }

    public boolean requiresLayout() {
        return false;
    }

    public boolean suppressException() {
        return true;
    }

    public void start() {
        for (Filter filter : filters) {
            filter.start();
        }
        this.started = true;
    }

    public void stop() {
        this.started = false;
        for (Filter filter : filters) {
            filter.stop();
        }
    }

    public boolean isStarted() {
        return started;
    }

    public static Layout createLayout(Node node) {
        Layout layout = null;
        for (Node child : node.getChildren()) {
            Object obj = child.getObject();
            if (obj != null && obj instanceof Layout) {
                layout = (Layout) obj;
            }
        }
        return layout;
    }

}
