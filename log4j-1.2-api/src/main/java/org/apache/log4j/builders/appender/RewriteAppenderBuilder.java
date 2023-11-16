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
package org.apache.log4j.builders.appender;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.config.Log4j1Configuration.APPENDER_REF_TAG;
import static org.apache.log4j.config.Log4j1Configuration.THRESHOLD_PARAM;
import static org.apache.log4j.xml.XmlConfiguration.FILTER_TAG;
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Appender;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.RewritePolicyAdapter;
import org.apache.log4j.bridge.RewritePolicyWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.builders.BuilderManager;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.w3c.dom.Element;

/**
 * Build a Rewrite Appender
 */
@Plugin(name = "org.apache.log4j.rewrite.RewriteAppender", category = CATEGORY)
public class RewriteAppenderBuilder extends AbstractBuilder implements AppenderBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String REWRITE_POLICY_TAG = "rewritePolicy";

    public RewriteAppenderBuilder() {}

    public RewriteAppenderBuilder(final String prefix, final Properties props) {
        super(prefix, props);
    }

    @Override
    public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
        final String name = getNameAttribute(appenderElement);
        final AtomicReference<List<String>> appenderRefs = new AtomicReference<>(new ArrayList<>());
        final AtomicReference<RewritePolicy> rewritePolicyHolder = new AtomicReference<>();
        final AtomicReference<String> level = new AtomicReference<>();
        final AtomicReference<Filter> filter = new AtomicReference<>();
        forEachElement(appenderElement.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case APPENDER_REF_TAG:
                    final Appender appender = config.findAppenderByReference(currentElement);
                    if (appender != null) {
                        appenderRefs.get().add(appender.getName());
                    }
                    break;
                case REWRITE_POLICY_TAG:
                    final RewritePolicy policy = config.parseRewritePolicy(currentElement);
                    if (policy != null) {
                        rewritePolicyHolder.set(policy);
                    }
                    break;
                case FILTER_TAG:
                    config.addFilter(filter, currentElement);
                    break;
                case PARAM_TAG:
                    if (getNameAttributeKey(currentElement).equalsIgnoreCase(THRESHOLD_PARAM)) {
                        set(THRESHOLD_PARAM, currentElement, level);
                    }
                    break;
            }
        });
        return createAppender(
                name,
                level.get(),
                appenderRefs.get().toArray(Strings.EMPTY_ARRAY),
                rewritePolicyHolder.get(),
                filter.get(),
                config);
    }

    @Override
    public Appender parseAppender(
            final String name,
            final String appenderPrefix,
            final String layoutPrefix,
            final String filterPrefix,
            final Properties props,
            final PropertiesConfiguration configuration) {
        final String appenderRef = getProperty(APPENDER_REF_TAG);
        final Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
        final String policyPrefix = appenderPrefix + ".rewritePolicy";
        final String className = getProperty(policyPrefix);
        final RewritePolicy policy = configuration
                .getBuilderManager()
                .parse(className, policyPrefix, props, configuration, BuilderManager.INVALID_REWRITE_POLICY);
        final String level = getProperty(THRESHOLD_PARAM);
        if (appenderRef == null) {
            LOGGER.error("No appender references configured for RewriteAppender {}", name);
            return null;
        }
        final Appender appender = configuration.parseAppender(props, appenderRef);
        if (appender == null) {
            LOGGER.error("Cannot locate Appender {}", appenderRef);
            return null;
        }
        return createAppender(name, level, new String[] {appenderRef}, policy, filter, configuration);
    }

    private <T extends Log4j1Configuration> Appender createAppender(
            final String name,
            final String level,
            final String[] appenderRefs,
            final RewritePolicy policy,
            final Filter filter,
            final T configuration) {
        if (appenderRefs.length == 0) {
            LOGGER.error("No appender references configured for RewriteAppender {}", name);
            return null;
        }
        final Level logLevel = OptionConverter.convertLevel(level, Level.TRACE);
        final AppenderRef[] refs = new AppenderRef[appenderRefs.length];
        int index = 0;
        for (final String appenderRef : appenderRefs) {
            refs[index++] = AppenderRef.createAppenderRef(appenderRef, logLevel, null);
        }
        final org.apache.logging.log4j.core.Filter rewriteFilter = buildFilters(level, filter);
        org.apache.logging.log4j.core.appender.rewrite.RewritePolicy rewritePolicy;
        if (policy instanceof RewritePolicyWrapper) {
            rewritePolicy = ((RewritePolicyWrapper) policy).getPolicy();
        } else {
            rewritePolicy = new RewritePolicyAdapter(policy);
        }
        return AppenderWrapper.adapt(
                RewriteAppender.createAppender(name, "true", refs, configuration, rewritePolicy, rewriteFilter));
    }
}
