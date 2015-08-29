package org.apache.logging.log4j.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *
 */
public class PropertiesUtil {

    /**
     * Extracts properties that start with or are equals to the specific prefix and returns them in a
     * new Properties object with the prefix removed.
     * @param properties The Properties to evaluate.
     * @param prefix The prefix to extract.
     * @return The subset of properties.
     */
    public static Properties extractSubset(Properties properties, String prefix) {
        Properties subset = new Properties();

        if (prefix == null || prefix.length() == 0) {
            return subset;
        }

        String prefixToMatch = prefix.charAt(prefix.length() - 1) != '.' ? prefix + '.' : prefix;

        List<String> keys = new ArrayList<>();

        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefixToMatch)) {
                subset.setProperty(key.substring(prefixToMatch.length()), properties.getProperty(key));
                keys.add(key);
            }
        }
        for (String key : keys) {
            properties.remove(key);
        }

        return subset;
    }
}
