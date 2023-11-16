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
package org.apache.log4j.xml;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertySetter;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Class Description goes here.
 */
public class XmlConfiguration extends Log4j1Configuration {

    private static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();

    private static final String CONFIGURATION_TAG = "log4j:configuration";
    private static final String OLD_CONFIGURATION_TAG = "configuration";
    private static final String RENDERER_TAG = "renderer";
    private static final String APPENDER_TAG = "appender";
    public static final String PARAM_TAG = "param";
    public static final String LAYOUT_TAG = "layout";
    private static final String CATEGORY = "category";
    private static final String LOGGER_ELEMENT = "logger";
    private static final String CATEGORY_FACTORY_TAG = "categoryFactory";
    private static final String LOGGER_FACTORY_TAG = "loggerFactory";
    public static final String NAME_ATTR = "name";
    private static final String CLASS_ATTR = "class";
    public static final String VALUE_ATTR = "value";
    private static final String ROOT_TAG = "root";
    private static final String LEVEL_TAG = "level";
    private static final String PRIORITY_TAG = "priority";
    public static final String FILTER_TAG = "filter";
    private static final String ERROR_HANDLER_TAG = "errorHandler";
    public static final String REF_ATTR = "ref";
    private static final String ADDITIVITY_ATTR = "additivity";
    private static final String CONFIG_DEBUG_ATTR = "configDebug";
    private static final String INTERNAL_DEBUG_ATTR = "debug";
    private static final String THRESHOLD_ATTR = "threshold";
    private static final String EMPTY_STR = "";
    private static final Class<?>[] ONE_STRING_PARAM = new Class[] {String.class};
    private static final String dbfKey = "javax.xml.parsers.DocumentBuilderFactory";
    private static final String THROWABLE_RENDERER_TAG = "throwableRenderer";

    public static final long DEFAULT_DELAY = 60000;

    /**
     * File name prefix for test configurations.
     */
    protected static final String TEST_PREFIX = "log4j-test";

    /**
     * File name prefix for standard configurations.
     */
    protected static final String DEFAULT_PREFIX = "log4j";

    // key: appenderName, value: appender
    private final Map<String, Appender> appenderMap;

    private final Properties props = null;

    public XmlConfiguration(
            final LoggerContext loggerContext, final ConfigurationSource source, final int monitorIntervalSeconds) {
        super(loggerContext, source, monitorIntervalSeconds);
        appenderMap = new HashMap<>();
    }

    public void addAppenderIfAbsent(Appender appender) {
        appenderMap.putIfAbsent(appender.getName(), appender);
    }

    /**
     * Configures log4j by reading in a log4j.dtd compliant XML
     * configuration file.
     */
    @Override
    public void doConfigure() throws FactoryConfigurationError {
        final ConfigurationSource source = getConfigurationSource();
        final ParseAction action = new ParseAction() {
            @Override
            @SuppressFBWarnings(
                    value = "XXE_DOCUMENT",
                    justification = "The `DocumentBuilder` is configured to not resolve external entities.")
            public Document parse(final DocumentBuilder parser) throws SAXException, IOException {
                @SuppressWarnings("resource")
                final // The ConfigurationSource and its caller manages the InputStream.
                InputSource inputSource = new InputSource(source.getInputStream());
                inputSource.setSystemId("dummy://log4j.dtd");
                return parser.parse(inputSource);
            }

            @Override
            public String toString() {
                return getConfigurationSource().getLocation();
            }
        };
        doConfigure(action);
    }

    private void doConfigure(final ParseAction action) throws FactoryConfigurationError {
        DocumentBuilderFactory dbf;
        try {
            LOGGER.debug("System property is : {}", OptionConverter.getSystemProperty(dbfKey, null));
            dbf = DocumentBuilderFactory.newInstance();
            LOGGER.debug("Standard DocumentBuilderFactory search succeeded.");
            LOGGER.debug("DocumentBuilderFactory is: " + dbf.getClass().getName());
        } catch (FactoryConfigurationError fce) {
            final Exception e = fce.getException();
            LOGGER.debug("Could not instantiate a DocumentBuilderFactory.", e);
            throw fce;
        }

        try {
            dbf.setValidating(true);

            final DocumentBuilder docBuilder = dbf.newDocumentBuilder();

            docBuilder.setErrorHandler(new SAXErrorHandler());
            docBuilder.setEntityResolver(new Log4jEntityResolver());

            final Document doc = action.parse(docBuilder);
            parse(doc.getDocumentElement());
        } catch (Exception e) {
            if (e instanceof InterruptedException || e instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            // I know this is miserable...
            LOGGER.error("Could not parse " + action.toString() + ".", e);
        }
    }

    @Override
    public Configuration reconfigure() {
        try {
            final ConfigurationSource source = getConfigurationSource().resetInputStream();
            if (source == null) {
                return null;
            }
            final XmlConfigurationFactory factory = new XmlConfigurationFactory();
            final XmlConfiguration config = (XmlConfiguration) factory.getConfiguration(getLoggerContext(), source);
            return config == null || config.getState() != State.INITIALIZING ? null : config;
        } catch (final IOException ex) {
            LOGGER.error("Cannot locate file {}: {}", getConfigurationSource(), ex);
        }
        return null;
    }

    /**
     * Delegates unrecognized content to created instance if it supports UnrecognizedElementParser.
     *
     * @param instance instance, may be null.
     * @param element  element, may not be null.
     * @param props    properties
     * @throws IOException thrown if configuration of owner object should be abandoned.
     */
    private void parseUnrecognizedElement(final Object instance, final Element element, final Properties props)
            throws Exception {
        boolean recognized = false;
        if (instance instanceof UnrecognizedElementHandler) {
            recognized = ((UnrecognizedElementHandler) instance).parseUnrecognizedElement(element, props);
        }
        if (!recognized) {
            LOGGER.warn("Unrecognized element {}", element.getNodeName());
        }
    }

    /**
     * Delegates unrecognized content to created instance if
     * it supports UnrecognizedElementParser and catches and
     * logs any exception.
     *
     * @param instance instance, may be null.
     * @param element  element, may not be null.
     * @param props    properties
     * @since 1.2.15
     */
    private void quietParseUnrecognizedElement(final Object instance, final Element element, final Properties props) {
        try {
            parseUnrecognizedElement(instance, element, props);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException || ex instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error("Error in extension content: ", ex);
        }
    }

    /**
     * Substitutes property value for any references in expression.
     *
     * @param value value from configuration file, may contain
     *              literal text, property references or both
     * @param props properties.
     * @return evaluated expression, may still contain expressions
     * if unable to expand.
     */
    public String subst(final String value, final Properties props) {
        try {
            return OptionConverter.substVars(value, props);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Could not perform variable substitution.", e);
            return value;
        }
    }

    /**
     * Sets a parameter based from configuration file content.
     *
     * @param elem       param element, may not be null.
     * @param propSetter property setter, may not be null.
     * @param props      properties
     * @since 1.2.15
     */
    public void setParameter(final Element elem, final PropertySetter propSetter, final Properties props) {
        final String name = subst(elem.getAttribute("name"), props);
        String value = (elem.getAttribute("value"));
        value = subst(OptionConverter.convertSpecialChars(value), props);
        propSetter.setProperty(name, value);
    }

    /**
     * Creates an object and processes any nested param elements
     * but does not call activateOptions.  If the class also supports
     * UnrecognizedElementParser, the parseUnrecognizedElement method
     * will be call for any child elements other than param.
     *
     * @param element       element, may not be null.
     * @param props         properties
     * @param expectedClass interface or class expected to be implemented
     *                      by created class
     * @return created class or null.
     * @throws Exception thrown if the contain object should be abandoned.
     * @since 1.2.15
     */
    public Object parseElement(
            final Element element, final Properties props, @SuppressWarnings("rawtypes") final Class expectedClass)
            throws Exception {
        final String clazz = subst(element.getAttribute("class"), props);
        final Object instance = OptionConverter.instantiateByClassName(clazz, expectedClass, null);

        if (instance != null) {
            final PropertySetter propSetter = new PropertySetter(instance);
            final NodeList children = element.getChildNodes();
            final int length = children.getLength();

            for (int loop = 0; loop < length; loop++) {
                final Node currentNode = children.item(loop);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    final Element currentElement = (Element) currentNode;
                    final String tagName = currentElement.getTagName();
                    if (tagName.equals("param")) {
                        setParameter(currentElement, propSetter, props);
                    } else {
                        parseUnrecognizedElement(instance, currentElement, props);
                    }
                }
            }
            return instance;
        }
        return null;
    }

    /**
     * Used internally to parse appenders by IDREF name.
     */
    private Appender findAppenderByName(final Document doc, final String appenderName) {
        Appender appender = appenderMap.get(appenderName);

        if (appender != null) {
            return appender;
        }
        // Endre's hack:
        Element element = null;
        final NodeList list = doc.getElementsByTagName("appender");
        for (int t = 0; t < list.getLength(); t++) {
            final Node node = list.item(t);
            final NamedNodeMap map = node.getAttributes();
            final Node attrNode = map.getNamedItem("name");
            if (appenderName.equals(attrNode.getNodeValue())) {
                element = (Element) node;
                break;
            }
        }
        // Hack finished.

        if (element == null) {

            LOGGER.error("No appender named [{}] could be found.", appenderName);
            return null;
        }
        appender = parseAppender(element);
        if (appender != null) {
            appenderMap.put(appenderName, appender);
        }
        return appender;
    }

    /**
     * Used internally to parse appenders by IDREF element.
     * @param appenderRef The Appender Reference Element.
     * @return The Appender.
     */
    public Appender findAppenderByReference(final Element appenderRef) {
        final String appenderName = subst(appenderRef.getAttribute(REF_ATTR));
        final Document doc = appenderRef.getOwnerDocument();
        return findAppenderByName(doc, appenderName);
    }

    /**
     * Used internally to parse an appender element.
     * @param appenderElement The Appender Element.
     * @return The Appender.
     */
    public Appender parseAppender(final Element appenderElement) {
        final String className = subst(appenderElement.getAttribute(CLASS_ATTR));
        LOGGER.debug("Class name: [" + className + ']');
        Appender appender = manager.parseAppender(className, appenderElement, this);
        if (appender == null) {
            appender = buildAppender(className, appenderElement);
        }
        return appender;
    }

    private Appender buildAppender(final String className, final Element appenderElement) {
        try {
            final Appender appender = LoaderUtil.newInstanceOf(className);
            final PropertySetter propSetter = new PropertySetter(appender);

            appender.setName(subst(appenderElement.getAttribute(NAME_ATTR)));
            final AtomicReference<Filter> filterChain = new AtomicReference<>();
            forEachElement(appenderElement.getChildNodes(), currentElement -> {
                // Parse appender parameters
                switch (currentElement.getTagName()) {
                    case PARAM_TAG:
                        setParameter(currentElement, propSetter);
                        break;
                    case LAYOUT_TAG:
                        appender.setLayout(parseLayout(currentElement));
                        break;
                    case FILTER_TAG:
                        addFilter(filterChain, currentElement);
                        break;
                    case ERROR_HANDLER_TAG:
                        parseErrorHandler(currentElement, appender);
                        break;
                    case APPENDER_REF_TAG:
                        final String refName = subst(currentElement.getAttribute(REF_ATTR));
                        if (appender instanceof AppenderAttachable) {
                            final AppenderAttachable aa = (AppenderAttachable) appender;
                            final Appender child = findAppenderByReference(currentElement);
                            LOGGER.debug(
                                    "Attaching appender named [{}] to appender named [{}].",
                                    refName,
                                    appender.getName());
                            aa.addAppender(child);
                        } else {
                            LOGGER.error(
                                    "Requesting attachment of appender named [{}] to appender named [{}]"
                                            + "which does not implement org.apache.log4j.spi.AppenderAttachable.",
                                    refName,
                                    appender.getName());
                        }
                        break;
                    default:
                        try {
                            parseUnrecognizedElement(appender, currentElement, props);
                        } catch (Exception ex) {
                            throw new ConsumerException(ex);
                        }
                }
            });
            final Filter head = filterChain.get();
            if (head != null) {
                appender.addFilter(head);
            }
            propSetter.activate();
            return appender;
        } catch (ConsumerException ex) {
            final Throwable t = ex.getCause();
            if (t instanceof InterruptedException || t instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error("Could not create an Appender. Reported error follows.", t);
        } catch (Exception oops) {
            if (oops instanceof InterruptedException || oops instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error("Could not create an Appender. Reported error follows.", oops);
        }
        return null;
    }

    public RewritePolicy parseRewritePolicy(final Element rewritePolicyElement) {
        final String className = subst(rewritePolicyElement.getAttribute(CLASS_ATTR));
        LOGGER.debug("Class name: [" + className + ']');
        RewritePolicy policy = manager.parseRewritePolicy(className, rewritePolicyElement, this);
        if (policy == null) {
            policy = buildRewritePolicy(className, rewritePolicyElement);
        }
        return policy;
    }

    private RewritePolicy buildRewritePolicy(String className, Element element) {
        try {
            final RewritePolicy policy = LoaderUtil.newInstanceOf(className);
            final PropertySetter propSetter = new PropertySetter(policy);

            forEachElement(element.getChildNodes(), currentElement -> {
                if (currentElement.getTagName().equalsIgnoreCase(PARAM_TAG)) {
                    setParameter(currentElement, propSetter);
                }
            });
            propSetter.activate();
            return policy;
        } catch (ConsumerException ex) {
            final Throwable t = ex.getCause();
            if (t instanceof InterruptedException || t instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error("Could not create an RewritePolicy. Reported error follows.", t);
        } catch (Exception oops) {
            if (oops instanceof InterruptedException || oops instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error("Could not create an RewritePolicy. Reported error follows.", oops);
        }
        return null;
    }

    /**
     * Used internally to parse an {@link ErrorHandler} element.
     */
    private void parseErrorHandler(Element element, Appender appender) {
        final ErrorHandler eh = (ErrorHandler) OptionConverter.instantiateByClassName(
                subst(element.getAttribute(CLASS_ATTR)), ErrorHandler.class, null);

        if (eh != null) {
            eh.setAppender(appender);

            final PropertySetter propSetter = new PropertySetter(eh);
            forEachElement(element.getChildNodes(), currentElement -> {
                final String tagName = currentElement.getTagName();
                if (tagName.equals(PARAM_TAG)) {
                    setParameter(currentElement, propSetter);
                }
            });
            propSetter.activate();
            appender.setErrorHandler(eh);
        }
    }

    /**
     * Used internally to parse a filter element.
     * @param filterElement The Filter Element.
     * @return The Filter.
     */
    public void addFilter(final AtomicReference<Filter> ref, final Element filterElement) {
        final Filter value = parseFilters(filterElement);
        ref.accumulateAndGet(value, FilterAdapter::addFilter);
    }

    /**
     * Used internally to parse a filter element.
     */
    public Filter parseFilters(final Element filterElement) {
        final String className = subst(filterElement.getAttribute(CLASS_ATTR));
        LOGGER.debug("Class name: [" + className + ']');
        Filter filter = manager.parseFilter(className, filterElement, this);
        if (filter == null) {
            filter = buildFilter(className, filterElement);
        }
        return filter;
    }

    private Filter buildFilter(final String className, final Element filterElement) {
        try {
            final Filter filter = LoaderUtil.newInstanceOf(className);
            final PropertySetter propSetter = new PropertySetter(filter);

            forEachElement(filterElement.getChildNodes(), currentElement -> {
                // Parse appender parameters
                switch (currentElement.getTagName()) {
                    case PARAM_TAG:
                        setParameter(currentElement, propSetter);
                        break;
                }
            });
            propSetter.activate();
            return filter;
        } catch (ConsumerException ex) {
            final Throwable t = ex.getCause();
            if (t instanceof InterruptedException || t instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error("Could not create an Filter. Reported error follows.", t);
        } catch (Exception oops) {
            if (oops instanceof InterruptedException || oops instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error("Could not create an Filter. Reported error follows.", oops);
        }
        return null;
    }

    /**
     * Used internally to parse an category element.
     */
    private void parseCategory(final Element loggerElement) {
        // Create a new org.apache.log4j.Category object from the <category> element.
        final String catName = subst(loggerElement.getAttribute(NAME_ATTR));
        final boolean additivity = OptionConverter.toBoolean(subst(loggerElement.getAttribute(ADDITIVITY_ATTR)), true);
        LoggerConfig loggerConfig = getLogger(catName);
        if (loggerConfig == null) {
            loggerConfig = new LoggerConfig(catName, org.apache.logging.log4j.Level.ERROR, additivity);
            addLogger(catName, loggerConfig);
        } else {
            loggerConfig.setAdditive(additivity);
        }
        parseChildrenOfLoggerElement(loggerElement, loggerConfig, false);
    }

    /**
     * Used internally to parse the root category element.
     */
    private void parseRoot(final Element rootElement) {
        final LoggerConfig root = getRootLogger();
        parseChildrenOfLoggerElement(rootElement, root, true);
    }

    /**
     * Used internally to parse the children of a LoggerConfig element.
     */
    private void parseChildrenOfLoggerElement(Element catElement, LoggerConfig loggerConfig, boolean isRoot) {

        final PropertySetter propSetter = new PropertySetter(loggerConfig);
        loggerConfig.getAppenderRefs().clear();
        forEachElement(catElement.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case APPENDER_REF_TAG: {
                    final Appender appender = findAppenderByReference(currentElement);
                    final String refName = subst(currentElement.getAttribute(REF_ATTR));
                    if (appender != null) {
                        LOGGER.debug(
                                "Adding appender named [{}] to loggerConfig [{}].", refName, loggerConfig.getName());
                        loggerConfig.addAppender(getAppender(refName), null, null);
                    } else {
                        LOGGER.debug("Appender named [{}] not found.", refName);
                    }
                    break;
                }
                case LEVEL_TAG:
                case PRIORITY_TAG: {
                    parseLevel(currentElement, loggerConfig, isRoot);
                    break;
                }
                case PARAM_TAG: {
                    setParameter(currentElement, propSetter);
                    break;
                }
                default: {
                    quietParseUnrecognizedElement(loggerConfig, currentElement, props);
                }
            }
        });
        propSetter.activate();
    }

    /**
     * Used internally to parse a layout element.
     * @param layoutElement The Layout Element.
     * @return The Layout.
     */
    public Layout parseLayout(final Element layoutElement) {
        final String className = subst(layoutElement.getAttribute(CLASS_ATTR));
        LOGGER.debug("Parsing layout of class: \"{}\"", className);
        Layout layout = manager.parseLayout(className, layoutElement, this);
        if (layout == null) {
            layout = buildLayout(className, layoutElement);
        }
        return layout;
    }

    private Layout buildLayout(final String className, final Element layout_element) {
        try {
            final Layout layout = LoaderUtil.newInstanceOf(className);
            final PropertySetter propSetter = new PropertySetter(layout);
            forEachElement(layout_element.getChildNodes(), currentElement -> {
                final String tagName = currentElement.getTagName();
                if (tagName.equals(PARAM_TAG)) {
                    setParameter(currentElement, propSetter);
                } else {
                    try {
                        parseUnrecognizedElement(layout, currentElement, props);
                    } catch (Exception ex) {
                        throw new ConsumerException(ex);
                    }
                }
            });

            propSetter.activate();
            return layout;
        } catch (Exception e) {
            final Throwable cause = e.getCause();
            if (e instanceof InterruptedException
                    || e instanceof InterruptedIOException
                    || cause instanceof InterruptedException
                    || cause instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.error("Could not create the Layout. Reported error follows.", e);
        }
        return null;
    }

    public TriggeringPolicy parseTriggeringPolicy(final Element policyElement) {
        final String className = subst(policyElement.getAttribute(CLASS_ATTR));
        LOGGER.debug("Parsing triggering policy of class: \"{}\"", className);
        return manager.parseTriggeringPolicy(className, policyElement, this);
    }

    /**
     * Used internally to parse a level  element.
     */
    private void parseLevel(Element element, LoggerConfig logger, boolean isRoot) {
        String catName = logger.getName();
        if (isRoot) {
            catName = "root";
        }

        final String priStr = subst(element.getAttribute(VALUE_ATTR));
        LOGGER.debug("Level value for {} is [{}].", catName, priStr);

        if (INHERITED.equalsIgnoreCase(priStr) || NULL.equalsIgnoreCase(priStr)) {
            if (isRoot) {
                LOGGER.error("Root level cannot be inherited. Ignoring directive.");
            } else {
                logger.setLevel(null);
            }
        } else {
            final String className = subst(element.getAttribute(CLASS_ATTR));
            final Level level;
            if (EMPTY_STR.equals(className)) {
                level = OptionConverter.toLevel(priStr, DEFAULT_LEVEL);
            } else {
                level = OptionConverter.toLevel(className, priStr, DEFAULT_LEVEL);
            }
            logger.setLevel(level != null ? level.getVersion2Level() : null);
        }
        LOGGER.debug("{} level set to {}", catName, logger.getLevel());
    }

    private void setParameter(Element element, PropertySetter propSetter) {
        final String name = subst(element.getAttribute(NAME_ATTR));
        String value = element.getAttribute(VALUE_ATTR);
        value = subst(OptionConverter.convertSpecialChars(value));
        propSetter.setProperty(name, value);
    }

    /**
     * Used internally to configure the log4j framework by parsing a DOM
     * tree of XML elements based on <a
     * href="doc-files/log4j.dtd">log4j.dtd</a>.
     */
    private void parse(Element element) {
        final String rootElementName = element.getTagName();

        if (!rootElementName.equals(CONFIGURATION_TAG)) {
            if (rootElementName.equals(OLD_CONFIGURATION_TAG)) {
                LOGGER.warn("The <" + OLD_CONFIGURATION_TAG + "> element has been deprecated.");
                LOGGER.warn("Use the <" + CONFIGURATION_TAG + "> element instead.");
            } else {
                LOGGER.error("DOM element is - not a <" + CONFIGURATION_TAG + "> element.");
                return;
            }
        }

        final String debugAttrib = subst(element.getAttribute(INTERNAL_DEBUG_ATTR));

        LOGGER.debug("debug attribute= \"" + debugAttrib + "\".");
        // if the log4j.dtd is not specified in the XML file, then the
        // "debug" attribute is returned as the empty string.
        String status = "error";
        if (!debugAttrib.equals("") && !debugAttrib.equals("null")) {
            status = OptionConverter.toBoolean(debugAttrib, true) ? "debug" : "error";
        } else {
            LOGGER.debug("Ignoring " + INTERNAL_DEBUG_ATTR + " attribute.");
        }

        final String confDebug = subst(element.getAttribute(CONFIG_DEBUG_ATTR));
        if (!confDebug.equals("") && !confDebug.equals("null")) {
            LOGGER.warn("The \"" + CONFIG_DEBUG_ATTR + "\" attribute is deprecated.");
            LOGGER.warn("Use the \"" + INTERNAL_DEBUG_ATTR + "\" attribute instead.");
            status = OptionConverter.toBoolean(confDebug, true) ? "debug" : "error";
        }

        final StatusConfiguration statusConfig = new StatusConfiguration().withStatus(status);
        statusConfig.initialize();

        final String threshold = subst(element.getAttribute(THRESHOLD_ATTR));
        if (threshold != null) {
            final org.apache.logging.log4j.Level level =
                    OptionConverter.convertLevel(threshold.trim(), org.apache.logging.log4j.Level.ALL);
            addFilter(ThresholdFilter.createFilter(level, Result.NEUTRAL, Result.DENY));
        }

        forEachElement(element.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
                case CATEGORY:
                case LOGGER_ELEMENT:
                    parseCategory(currentElement);
                    break;
                case ROOT_TAG:
                    parseRoot(currentElement);
                    break;
                case RENDERER_TAG:
                    LOGGER.warn("Log4j 1 renderers are not supported by Log4j 2 and will be ignored.");
                    break;
                case THROWABLE_RENDERER_TAG:
                    LOGGER.warn("Log4j 1 throwable renderers are not supported by Log4j 2 and will be ignored.");
                    break;
                case CATEGORY_FACTORY_TAG:
                case LOGGER_FACTORY_TAG:
                    LOGGER.warn("Log4j 1 logger factories are not supported by Log4j 2 and will be ignored.");
                    break;
                case APPENDER_TAG:
                    final Appender appender = parseAppender(currentElement);
                    appenderMap.put(appender.getName(), appender);
                    addAppender(AppenderAdapter.adapt(appender));
                    break;
                default:
                    quietParseUnrecognizedElement(null, currentElement, props);
            }
        });
    }

    private String subst(final String value) {
        return getStrSubstitutor().replace(value);
    }

    public static void forEachElement(final NodeList list, final Consumer<Element> consumer) {
        IntStream.range(0, list.getLength())
                .mapToObj(list::item)
                .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
                .forEach(node -> consumer.accept((Element) node));
    }

    private interface ParseAction {
        Document parse(final DocumentBuilder parser) throws SAXException, IOException;
    }

    private static class SAXErrorHandler implements org.xml.sax.ErrorHandler {
        private static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();

        @Override
        public void error(final SAXParseException ex) {
            emitMessage("Continuable parsing error ", ex);
        }

        @Override
        public void fatalError(final SAXParseException ex) {
            emitMessage("Fatal parsing error ", ex);
        }

        @Override
        public void warning(final SAXParseException ex) {
            emitMessage("Parsing warning ", ex);
        }

        private static void emitMessage(final String msg, final SAXParseException ex) {
            LOGGER.warn("{} {} and column {}", msg, ex.getLineNumber(), ex.getColumnNumber());
            LOGGER.warn(ex.getMessage(), ex.getException());
        }
    }

    private static class ConsumerException extends RuntimeException {

        ConsumerException(final Exception ex) {
            super(ex);
        }
    }
}
