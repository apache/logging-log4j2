package org.apache.logging.log4j.core.lookup;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.config.plugins.Plugin;

/**
 * Maps JVM input arguments (but not main arguments) using JMX to acquire JVM arguments.
 * 
 * @see java.lang.management.RuntimeMXBean#getInputArguments()
 * @since 2.1
 */
@Plugin(name = "jmx-input", category = "Lookup")
public class JmxRuntimeInputArgumentsLookup extends MapLookup {

    static {
        List<String> argsList = ManagementFactory.getRuntimeMXBean().getInputArguments();
        JMX_SINGLETON = new JmxRuntimeInputArgumentsLookup(MapLookup.toMap(argsList));
    }

    public static final JmxRuntimeInputArgumentsLookup JMX_SINGLETON;

    public JmxRuntimeInputArgumentsLookup(Map<String, String> map) {
        super(map);
    }

}
