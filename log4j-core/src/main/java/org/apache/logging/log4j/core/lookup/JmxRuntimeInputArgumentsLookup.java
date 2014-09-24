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
@Plugin(name = "jvmrunargs", category = StrLookup.CATEGORY)
public class JmxRuntimeInputArgumentsLookup extends MapLookup {

    static {
        List<String> argsList = ManagementFactory.getRuntimeMXBean().getInputArguments();
        JMX_SINGLETON = new JmxRuntimeInputArgumentsLookup(MapLookup.toMap(argsList));
    }

    public static final JmxRuntimeInputArgumentsLookup JMX_SINGLETON;

    /**
     * Constructor when used directly as a plugin.
     */
    public JmxRuntimeInputArgumentsLookup() {
        super();
    }

    public JmxRuntimeInputArgumentsLookup(Map<String, String> map) {
        super(map);
    }

}
