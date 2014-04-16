/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.camel;

import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.camel.processor.DefaultExchangeFormatter;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;

/**
 * Log4j Component for Camel.
 */
public class LogComponent extends UriEndpointComponent {

    private static final Logger LOGGER = LogManager.getLogger();

    private ExchangeFormatter exchangeFormatter;

    public LogComponent() {
        super(LogEndpoint.class);
    }

    @Override
    protected Endpoint createEndpoint(final String uri, final String remaining, final Map<String, Object> parameters) throws Exception {
        final Level level = getLevel(parameters);
        final LogEndpoint endpoint = new LogEndpoint(uri, this);
        endpoint.setLevel(level.name());
        setProperties(endpoint, parameters);

        final LoggerWrapper logger = new LoggerWrapper();
        logger.setLogger(getLogger(uri, remaining, parameters));
        logger.setLevel(getLevel(parameters));
        if (endpoint.getMarker() != null) {
            logger.setMarker(MarkerManager.getMarker(endpoint.getMarker()));
        }
        return configureEndpoint(endpoint, logger, parameters);
    }

    private Logger getLogger(final String uri, final String remaining, final Map<String, Object> parameters) {
        final Logger providedLogger = getAndRemoveOrResolveReferenceParameter(parameters, "logger", Logger.class);
        if (providedLogger != null) {
            return providedLogger;
        }
        final Map<String, Logger> availableLoggers = getCamelContext().getRegistry().findByTypeWithName(Logger.class);
        if (availableLoggers.size() == 1) {
            final Logger logger = availableLoggers.values().iterator().next();
            LOGGER.info("Using custom logger: {}", logger);
            return logger;
        }
        LOGGER.info("More than one {} instance found in the registry. Falling back to creating logger from URI {}",
                Logger.class.getName(), uri);
        return LogManager.getLogger(remaining);
    }

    private Level getLevel(final Map<String, Object> parameters) {
        final String levelName = getAndRemoveParameter(parameters, "level", String.class, "INFO");
        return Level.toLevel(levelName);
    }

    private Endpoint configureEndpoint(final LogEndpoint endpoint, final LoggerWrapper logger, final Map<String, Object> parameters) throws Exception {
        final Processor processor = new LogProcessor(logger, configureExchangeFormatter(parameters));
        endpoint.setLogger(processor);
        return endpoint;
    }

    private ExchangeFormatter configureExchangeFormatter(final Map<String, Object> parameters) throws Exception {
        // first, try to use the user-specified formatter (or the one picked up from the Registry and transferred to
        // the property by a previous endpoint initialisation); if null, try to pick it up from the Registry now
        ExchangeFormatter localFormatter = exchangeFormatter;
        if (localFormatter == null) {
            localFormatter = getCamelContext().getRegistry().lookupByNameAndType("logFormatter", ExchangeFormatter.class);
            if (localFormatter != null) {
                exchangeFormatter = localFormatter;
                setProperties(exchangeFormatter, parameters);
            }
        }
        // if no formatter is available in the Registry, create a local one of the default type, for a single use
        if (localFormatter == null) {
            localFormatter = new DefaultExchangeFormatter();
            setProperties(localFormatter, parameters);
        }
        return localFormatter;
    }

    public ExchangeFormatter getExchangeFormatter() {
        return exchangeFormatter;
    }

    public void setExchangeFormatter(final ExchangeFormatter exchangeFormatter) {
        this.exchangeFormatter = exchangeFormatter;
    }
}
