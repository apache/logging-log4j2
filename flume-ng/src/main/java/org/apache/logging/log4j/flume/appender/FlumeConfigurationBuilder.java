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
package org.apache.logging.log4j.flume.appender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.flume.Channel;
import org.apache.flume.ChannelFactory;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.Sink;
import org.apache.flume.SinkFactory;
import org.apache.flume.SinkProcessor;
import org.apache.flume.SinkRunner;
import org.apache.flume.Source;
import org.apache.flume.SourceFactory;
import org.apache.flume.SourceRunner;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.channel.ChannelSelectorFactory;
import org.apache.flume.channel.DefaultChannelFactory;
import org.apache.flume.conf.BasicConfigurationConstants;
import org.apache.flume.conf.ComponentConfiguration;
import org.apache.flume.conf.Configurables;
import org.apache.flume.conf.FlumeConfiguration;
import org.apache.flume.conf.FlumeConfigurationError;
import org.apache.flume.conf.channel.ChannelSelectorConfiguration;
import org.apache.flume.conf.file.SimpleNodeConfiguration;
import org.apache.flume.conf.sink.SinkConfiguration;
import org.apache.flume.conf.sink.SinkGroupConfiguration;
import org.apache.flume.conf.source.SourceConfiguration;
import org.apache.flume.node.NodeConfiguration;
import org.apache.flume.node.nodemanager.NodeConfigurationAware;
import org.apache.flume.sink.DefaultSinkFactory;
import org.apache.flume.sink.DefaultSinkProcessor;
import org.apache.flume.sink.SinkGroup;
import org.apache.flume.source.DefaultSourceFactory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * See Flume's PropertiesFileConfigurationProvider. This class would extend that if it were possible.
 */

public class FlumeConfigurationBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final ChannelFactory channelFactory = new DefaultChannelFactory();
    private final SourceFactory sourceFactory = new DefaultSourceFactory();
    private final SinkFactory sinkFactory = new DefaultSinkFactory();

    public NodeConfiguration load(final String name, final Properties props,
                                  final NodeConfigurationAware configurationAware) {
        final NodeConfiguration conf = new SimpleNodeConfiguration();
        FlumeConfiguration fconfig;
        try {
            fconfig = new FlumeConfiguration(props);
            final List<FlumeConfigurationError> errors = fconfig.getConfigurationErrors();
            if (errors.size() > 0) {
                boolean isError = false;
                for (final FlumeConfigurationError error : errors) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Component: ").append(error.getComponentName()).append(" ");
                    sb.append("Key: ").append(error.getKey()).append(" ");
                    sb.append(error.getErrorType().name()).append(" - ").append(error.getErrorType().getError());
                    switch (error.getErrorOrWarning()) {
                        case ERROR:
                            isError = true;
                            LOGGER.error(sb.toString());
                            break;
                        case WARNING:
                            LOGGER.warn(sb.toString());
                            break;
                    }
                }
                if (isError) {
                    throw new ConfigurationException("Unable to configure Flume due to errors");
                }
            }
        } catch (final RuntimeException ex) {
            printProps(props);
            throw ex;
        }

        final FlumeConfiguration.AgentConfiguration agentConf = fconfig.getConfigurationFor(name);

        if (agentConf != null) {

            loadChannels(agentConf, conf);
            loadSources(agentConf, conf);
            loadSinks(agentConf, conf);

            //configurationAware.startAllComponents(conf);
        } else {
            LOGGER.warn("No configuration found for: {}", name);
        }
        return conf;
    }

    private void printProps(final Properties props) {
        for (final String key : new TreeSet<String>(props.stringPropertyNames())) {
            LOGGER.error(key + "=" + props.getProperty(key));
        }
    }

    protected void loadChannels(final FlumeConfiguration.AgentConfiguration agentConf, final NodeConfiguration conf) {
        LOGGER.info("Creating channels");
        final Set<String> channels = agentConf.getChannelSet();
        final Map<String, ComponentConfiguration> compMap = agentConf.getChannelConfigMap();
        for (final String chName : channels) {
            final ComponentConfiguration comp = compMap.get(chName);
            if (comp != null) {
                final Channel channel = channelFactory.create(comp.getComponentName(), comp.getType());

                Configurables.configure(channel, comp);

                conf.getChannels().put(comp.getComponentName(), channel);
            }
        }

        for (final String ch : channels) {
            final Context context = agentConf.getChannelContext().get(ch);
            if (context != null) {
                final Channel channel = channelFactory.create(ch,
                    context.getString(BasicConfigurationConstants.CONFIG_TYPE));
                Configurables.configure(channel, context);
                conf.getChannels().put(ch, channel);
                LOGGER.info("created channel " + ch);
            }
        }
    }

    protected void loadSources(final FlumeConfiguration.AgentConfiguration agentConf, final NodeConfiguration conf) {

        final Set<String> sources = agentConf.getSourceSet();
        final Map<String, ComponentConfiguration> compMap = agentConf.getSourceConfigMap();
        for (final String sourceName : sources) {
            final ComponentConfiguration comp = compMap.get(sourceName);
            if (comp != null) {
                final SourceConfiguration config = (SourceConfiguration) comp;

                final Source source = sourceFactory.create(comp.getComponentName(), comp.getType());

                Configurables.configure(source, config);
                final Set<String> channelNames = config.getChannels();
                final List<Channel> channels = new ArrayList<Channel>();
                for (final String chName : channelNames) {
                    channels.add(conf.getChannels().get(chName));
                }

                final ChannelSelectorConfiguration selectorConfig = config.getSelectorConfiguration();

                final ChannelSelector selector = ChannelSelectorFactory.create(channels, selectorConfig);

                final ChannelProcessor channelProcessor = new ChannelProcessor(selector);
                Configurables.configure(channelProcessor, config);

                source.setChannelProcessor(channelProcessor);
                conf.getSourceRunners().put(comp.getComponentName(), SourceRunner.forSource(source));
            }
        }
        final Map<String, Context> sourceContexts = agentConf.getSourceContext();

        for (final String src : sources) {
            final Context context = sourceContexts.get(src);
            if (context != null) {
                final Source source = sourceFactory.create(src,
                    context.getString(BasicConfigurationConstants.CONFIG_TYPE));
                final List<Channel> channels = new ArrayList<Channel>();
                Configurables.configure(source, context);
                final String[] channelNames =
                    context.getString(BasicConfigurationConstants.CONFIG_CHANNELS).split("\\s+");
                for (final String chName : channelNames) {
                    channels.add(conf.getChannels().get(chName));
                }

                final Map<String, String> selectorConfig = context.getSubProperties(
                    BasicConfigurationConstants.CONFIG_SOURCE_CHANNELSELECTOR_PREFIX);

                final ChannelSelector selector = ChannelSelectorFactory.create(channels, selectorConfig);

                final ChannelProcessor channelProcessor = new ChannelProcessor(selector);
                Configurables.configure(channelProcessor, context);

                source.setChannelProcessor(channelProcessor);
                conf.getSourceRunners().put(src, SourceRunner.forSource(source));
            }
        }
    }

    protected void loadSinks(final FlumeConfiguration.AgentConfiguration agentConf, final NodeConfiguration conf) {
        final Set<String> sinkNames = agentConf.getSinkSet();
        final Map<String, ComponentConfiguration> compMap = agentConf.getSinkConfigMap();
        final Map<String, Sink> sinks = new HashMap<String, Sink>();
        for (final String sinkName : sinkNames) {
            final ComponentConfiguration comp = compMap.get(sinkName);
            if (comp != null) {
                final SinkConfiguration config = (SinkConfiguration) comp;
                final Sink sink = sinkFactory.create(comp.getComponentName(), comp.getType());

                Configurables.configure(sink, config);

                sink.setChannel(conf.getChannels().get(config.getChannel()));
                sinks.put(comp.getComponentName(), sink);
            }
        }

        final Map<String, Context> sinkContexts = agentConf.getSinkContext();
        for (final String sinkName : sinkNames) {
            final Context context = sinkContexts.get(sinkName);
            if (context != null) {
                final Sink sink = sinkFactory.create(sinkName,
                    context.getString(BasicConfigurationConstants.CONFIG_TYPE));
                Configurables.configure(sink, context);

                sink.setChannel(conf.getChannels().get(context.getString(BasicConfigurationConstants.CONFIG_CHANNEL)));
                sinks.put(sinkName, sink);
            }
        }

        loadSinkGroups(agentConf, sinks, conf);
    }

    protected void loadSinkGroups(final FlumeConfiguration.AgentConfiguration agentConf,
                                  final Map<String, Sink> sinks, final NodeConfiguration conf) {
        final Set<String> sinkgroupNames = agentConf.getSinkgroupSet();
        final Map<String, ComponentConfiguration> compMap = agentConf.getSinkGroupConfigMap();
        final Map<String, String> usedSinks = new HashMap<String, String>();
        for (final String groupName : sinkgroupNames) {
            final ComponentConfiguration comp = compMap.get(groupName);
            if (comp != null) {
                final SinkGroupConfiguration groupConf = (SinkGroupConfiguration) comp;
                final List<String> groupSinkList = groupConf.getSinks();
                final List<Sink> groupSinks = new ArrayList<Sink>();
                for (final String sink : groupSinkList) {
                    final Sink s = sinks.remove(sink);
                    if (s == null) {
                        final String sinkUser = usedSinks.get(sink);
                        if (sinkUser != null) {
                            throw new ConfigurationException(String.format(
                                "Sink %s of group %s already in use by group %s", sink, groupName, sinkUser));
                        }
                        throw new ConfigurationException(String.format(
                                "Sink %s of group %s does not exist or is not properly configured", sink,
                                groupName));
                    }
                    groupSinks.add(s);
                    usedSinks.put(sink, groupName);
                }
                final SinkGroup group = new SinkGroup(groupSinks);
                Configurables.configure(group, groupConf);
                conf.getSinkRunners().put(comp.getComponentName(), new SinkRunner(group.getProcessor()));
            }
        }
        // add any unassigned sinks to solo collectors
        for (final Map.Entry<String, Sink> entry : sinks.entrySet()) {
            if (!usedSinks.containsValue(entry.getKey())) {
                final SinkProcessor pr = new DefaultSinkProcessor();
                final List<Sink> sinkMap = new ArrayList<Sink>();
                sinkMap.add(entry.getValue());
                pr.setSinks(sinkMap);
                Configurables.configure(pr, new Context());
                conf.getSinkRunners().put(entry.getKey(), new SinkRunner(pr));
            }
        }
    }
}
