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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * See Flume's PropertiesFileConfigurationProvider. This class would extend that if it were possible.
 */

public class FlumeConfigurationBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final ChannelFactory channelFactory = new DefaultChannelFactory();
    private final SourceFactory sourceFactory = new DefaultSourceFactory();
    private final SinkFactory sinkFactory = new DefaultSinkFactory();

    public NodeConfiguration load(String name, Properties props, NodeConfigurationAware configurationAware) {
        NodeConfiguration conf = new SimpleNodeConfiguration();
        FlumeConfiguration fconfig;
        try {
            fconfig = new FlumeConfiguration(props);
            List<FlumeConfigurationError> errors = fconfig.getConfigurationErrors();
            if (errors.size() > 0) {
                boolean isError = false;
                for (FlumeConfigurationError error : errors) {
                    StringBuilder sb = new StringBuilder();
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
        } catch (RuntimeException ex) {
            printProps(props);
            throw ex;
        }

        FlumeConfiguration.AgentConfiguration agentConf = fconfig.getConfigurationFor(name);

        if (agentConf != null) {

            loadChannels(agentConf, conf);
            loadSources(agentConf, conf);
            loadSinks(agentConf, conf);

            configurationAware.startAllComponents(conf);
        } else {
            LOGGER.warn("No configuration found for: {}", name);
        }
        return conf;
    }

    private void printProps(Properties props) {
        for (String key : new TreeSet<String>(props.stringPropertyNames())) {
            LOGGER.error(key + "=" + props.getProperty(key));
        }
    }

    protected void loadChannels(FlumeConfiguration.AgentConfiguration agentConf, NodeConfiguration conf) {
        LOGGER.info("Creating channels");
        Set<String> channels = agentConf.getChannelSet();
        Map<String, ComponentConfiguration> compMap = agentConf.getChannelConfigMap();
        for (String chName : channels) {
            ComponentConfiguration comp = compMap.get(chName);
            if (comp != null) {
                Channel channel = channelFactory.create(comp.getComponentName(), comp.getType());

                Configurables.configure(channel, comp);

                conf.getChannels().put(comp.getComponentName(), channel);
            }
        }

        for (String ch : channels) {
            Context context = agentConf.getChannelContext().get(ch);
            if (context != null) {
                Channel channel = channelFactory.create(ch, context.getString(BasicConfigurationConstants.CONFIG_TYPE));
                Configurables.configure(channel, context);
                conf.getChannels().put(ch, channel);
                LOGGER.info("created channel " + ch);
            }
        }
    }

    protected void loadSources(FlumeConfiguration.AgentConfiguration agentConf, NodeConfiguration conf) {

        Set<String> sources = agentConf.getSourceSet();
        Map<String, ComponentConfiguration> compMap = agentConf.getSourceConfigMap();
        for (String sourceName : sources) {
            ComponentConfiguration comp = compMap.get(sourceName);
            if (comp != null) {
                SourceConfiguration config = (SourceConfiguration) comp;

                Source source = sourceFactory.create(comp.getComponentName(), comp.getType());

                Configurables.configure(source, config);
                Set<String> channelNames = config.getChannels();
                List<Channel> channels = new ArrayList<Channel>();
                for (String chName : channelNames) {
                    channels.add(conf.getChannels().get(chName));
                }

                ChannelSelectorConfiguration selectorConfig = config.getSelectorConfiguration();

                ChannelSelector selector = ChannelSelectorFactory.create(channels, selectorConfig);

                ChannelProcessor channelProcessor = new ChannelProcessor(selector);
                Configurables.configure(channelProcessor, config);

                source.setChannelProcessor(channelProcessor);
                conf.getSourceRunners().put(comp.getComponentName(), SourceRunner.forSource(source));
            }
        }
        Map<String, Context> sourceContexts = agentConf.getSourceContext();

        for (String src : sources) {
            Context context = sourceContexts.get(src);
            if (context != null){
                Source source = sourceFactory.create(src, context.getString(BasicConfigurationConstants.CONFIG_TYPE));
                List<Channel> channels = new ArrayList<Channel>();
                Configurables.configure(source, context);
                String[] channelNames = context.getString(BasicConfigurationConstants.CONFIG_CHANNELS).split("\\s+");
                for (String chName : channelNames) {
                    channels.add(conf.getChannels().get(chName));
                }

                Map<String, String> selectorConfig = context.getSubProperties(
                    BasicConfigurationConstants.CONFIG_SOURCE_CHANNELSELECTOR_PREFIX);

                ChannelSelector selector = ChannelSelectorFactory.create(channels, selectorConfig);

                ChannelProcessor channelProcessor = new ChannelProcessor(selector);
                Configurables.configure(channelProcessor, context);

                source.setChannelProcessor(channelProcessor);
                conf.getSourceRunners().put(src, SourceRunner.forSource(source));
            }
        }
    }

    protected void loadSinks(FlumeConfiguration.AgentConfiguration agentConf, NodeConfiguration conf) {
        Set<String> sinkNames = agentConf.getSinkSet();
        Map<String, ComponentConfiguration> compMap = agentConf.getSinkConfigMap();
        Map<String, Sink> sinks = new HashMap<String, Sink>();
        for (String sinkName : sinkNames) {
            ComponentConfiguration comp = compMap.get(sinkName);
            if (comp != null) {
                SinkConfiguration config = (SinkConfiguration) comp;
                Sink sink = sinkFactory.create(comp.getComponentName(), comp.getType());

                Configurables.configure(sink, config);

                sink.setChannel(conf.getChannels().get(config.getChannel()));
                sinks.put(comp.getComponentName(), sink);
            }
        }

        Map<String, Context> sinkContexts = agentConf.getSinkContext();
        for (String sinkName : sinkNames) {
            Context context = sinkContexts.get(sinkName);
            if (context != null) {
                Sink sink = sinkFactory.create(sinkName, context.getString(BasicConfigurationConstants.CONFIG_TYPE));
                Configurables.configure(sink, context);

                sink.setChannel(conf.getChannels().get(context.getString(BasicConfigurationConstants.CONFIG_CHANNEL)));
                sinks.put(sinkName, sink);
            }
        }

        loadSinkGroups(agentConf, sinks, conf);
    }

    protected void loadSinkGroups(FlumeConfiguration.AgentConfiguration agentConf,
                                  Map<String, Sink> sinks, NodeConfiguration conf) {
        Set<String> sinkgroupNames = agentConf.getSinkgroupSet();
        Map<String, ComponentConfiguration> compMap = agentConf.getSinkGroupConfigMap();
        Map<String, String> usedSinks = new HashMap<String, String>();
        for (String groupName : sinkgroupNames) {
            ComponentConfiguration comp = compMap.get(groupName);
            if (comp != null) {
                SinkGroupConfiguration groupConf = (SinkGroupConfiguration) comp;
                List<String> groupSinkList = groupConf.getSinks();
                List<Sink> groupSinks = new ArrayList<Sink>();
                for (String sink : groupSinkList) {
                    Sink s = sinks.remove(sink);
                    if (s == null) {
                        String sinkUser = usedSinks.get(sink);
                        if (sinkUser != null) {
                            throw new ConfigurationException(String.format(
                                "Sink %s of group %s already in use by group %s", sink, groupName, sinkUser));
                        } else {
                            throw new ConfigurationException(String.format(
                                "Sink %s of group %s does not exist or is not properly configured", sink,
                                groupName));
                        }
                    }
                    groupSinks.add(s);
                    usedSinks.put(sink, groupName);
                }
                SinkGroup group = new SinkGroup(groupSinks);
                Configurables.configure(group, groupConf);
                conf.getSinkRunners().put(comp.getComponentName(), new SinkRunner(group.getProcessor()));
            }
        }
        // add any unasigned sinks to solo collectors
        for (Map.Entry<String, Sink> entry : sinks.entrySet()) {
            if (!usedSinks.containsValue(entry.getKey())) {
                SinkProcessor pr = new DefaultSinkProcessor();
                List<Sink> sinkMap = new ArrayList<Sink>();
                sinkMap.add(entry.getValue());
                pr.setSinks(sinkMap);
                Configurables.configure(pr, new Context());
                conf.getSinkRunners().put(entry.getKey(), new SinkRunner(pr));
            }
        }
    }
}
