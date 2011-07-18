package org.apache.logging.log4j.core.selector;

import org.apache.logging.log4j.core.LoggerContext;

/**
 *
 */
public interface NamedContextSelector extends ContextSelector {

    LoggerContext locateContext(String name, String configLocation);

    LoggerContext removeContext(String name);
}
