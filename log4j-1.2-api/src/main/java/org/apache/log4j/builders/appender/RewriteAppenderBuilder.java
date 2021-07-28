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
package org.apache.log4j.builders.appender;

import org.apache.log4j.Appender;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.RewritePolicyAdapter;
import org.apache.log4j.bridge.RewritePolicyWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.builders.Holder;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.apache.log4j.builders.BuilderManager.CATEGORY;
import static org.apache.log4j.config.Log4j1Configuration.APPENDER_REF_TAG;
import static org.apache.log4j.config.Log4j1Configuration.THRESHOLD_PARAM;
import static org.apache.log4j.xml.XmlConfiguration.FILTER_TAG;
import static org.apache.log4j.xml.XmlConfiguration.NAME_ATTR;
import static org.apache.log4j.xml.XmlConfiguration.PARAM_TAG;
import static org.apache.log4j.xml.XmlConfiguration.VALUE_ATTR;
import static org.apache.log4j.xml.XmlConfiguration.forEachElement;


/**
 * Build an Asynch Appender
 */
@Plugin(name = "org.apache.log4j.rewrite.RewriteAppender", category = CATEGORY)
public class RewriteAppenderBuilder extends AbstractBuilder implements AppenderBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String REWRITE_POLICY_TAG = "rewritePolicy";

    public RewriteAppenderBuilder() {
    }

    public RewriteAppenderBuilder(String prefix, Properties props) {
        super(prefix, props);
    }

    @Override
    public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
        String name = appenderElement.getAttribute(NAME_ATTR);
        Holder<List<String>> appenderRefs = new Holder<>(new ArrayList<>());
        Holder<RewritePolicy> rewritePolicyHolder = new Holder<>();
        Holder<String> level = new Holder<>();
        Holder<Filter> filter = new Holder<>();
        forEachElement(appenderElement.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case APPENDER_REF_TAG:
                    Appender appender = config.findAppenderByReference(currentElement);
                    if (appender != null) {
                        appenderRefs.get().add(appender.getName());
                    }
                    break;
                case REWRITE_POLICY_TAG: {
                    RewritePolicy policy = config.parseRewritePolicy(currentElement);
                    if (policy != null) {
                        rewritePolicyHolder.set(policy);
                    }
                    break;
                }
                case FILTER_TAG: {
                    filter.set(config.parseFilters(currentElement));
                    break;
                }
                case PARAM_TAG: {
                    if (currentElement.getAttribute(NAME_ATTR).equalsIgnoreCase(THRESHOLD_PARAM)) {
                        String value = currentElement.getAttribute(VALUE_ATTR);
                        if (value == null) {
                            LOGGER.warn("No value supplied for Threshold parameter, ignoring.");
                        } else {
                            level.set(value);
                        }
                    }
                    break;
                }
            }
        });
        return createAppender(name, level.get(), appenderRefs.get().toArray(Strings.EMPTY_ARRAY), rewritePolicyHolder.get(),
                filter.get(), config);
    }

    @Override
    public Appender parseAppender(final String name, final String appenderPrefix, final String layoutPrefix,
            final String filterPrefix, final Properties props, final PropertiesConfiguration configuration) {
        String appenderRef = getProperty(APPENDER_REF_TAG);
        Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
        String policyPrefix = appenderPrefix + ".rewritePolicy";
        String className = getProperty(policyPrefix);
        RewritePolicy policy = configuration.getBuilderManager().parseRewritePolicy(className, policyPrefix,
                props, configuration);
        String level = getProperty(THRESHOLD_PARAM);
        if (appenderRef == null) {
            LOGGER.warn("No appender references configured for AsyncAppender {}", name);
            return null;
        }
        Appender appender = configuration.parseAppender(props, appenderRef);
        if (appender == null) {
            LOGGER.warn("Cannot locate Appender {}", appenderRef);
            return null;
        }
        return createAppender(name, level, new String[] {appenderRef}, policy, filter, configuration);
    }

    private <T extends Log4j1Configuration> Appender createAppender(String name, String level,
            String[] appenderRefs, RewritePolicy policy, Filter filter, T configuration) {
        org.apache.logging.log4j.Level logLevel = OptionConverter.convertLevel(level,
                org.apache.logging.log4j.Level.TRACE);
        AppenderRef[] refs = new AppenderRef[appenderRefs.length];
        int index = 0;
        for (String appenderRef : appenderRefs) {
            refs[index++] = AppenderRef.createAppenderRef(appenderRef, logLevel, null);
        }
        org.apache.logging.log4j.core.Filter rewriteFilter = buildFilters(level, filter);
        org.apache.logging.log4j.core.appender.rewrite.RewritePolicy rewritePolicy;
        if (policy instanceof RewritePolicyWrapper) {
            rewritePolicy = ((RewritePolicyWrapper) policy).getPolicy();
        } else {
            rewritePolicy = new RewritePolicyAdapter(policy);
        }
        return new AppenderWrapper(RewriteAppender.createAppender(name, "true", refs, configuration,
                rewritePolicy, rewriteFilter));
    }
}
