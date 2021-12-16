package org.apache.logging.log4j.core.lookup;

import java.util.Map;
import java.util.Properties;

/**
 * {@link RuntimeStrSubstitutor} is a {@link StrSubstitutor} which only supports recursive evaluation of lookups.
 * This can be dangerous when combined with user-provided inputs, and should only be used on data directly from
 * a configuration.
 */
public final class ConfigurationStrSubstitutor extends StrSubstitutor {

    public ConfigurationStrSubstitutor() {
    }

    public ConfigurationStrSubstitutor(final Map<String, String> valueMap) {
        super(valueMap);
    }

    public ConfigurationStrSubstitutor(final Properties properties) {
        super(properties);
    }

    public ConfigurationStrSubstitutor(final StrLookup lookup) {
        super(lookup);
    }

    public ConfigurationStrSubstitutor(final StrSubstitutor other) {
        super(other);
    }

    @Override
    boolean isRecursiveEvaluationAllowed() {
        return true;
    }

    @Override
    void setRecursiveEvaluationAllowed(final boolean recursiveEvaluationAllowed) {
        throw new UnsupportedOperationException(
                "recursiveEvaluationAllowed cannot be modified within ConfigurationStrSubstitutor");
    }

    @Override
    public String toString() {
        return "ConfigurationStrSubstitutor{" + super.toString() + "}";
    }
}
