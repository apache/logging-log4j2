package org.apache.logging.log4j.core.lookup;

import java.util.Map;
import java.util.Properties;

/**
 * {@link RuntimeStrSubstitutor} is a {@link StrSubstitutor} which only supports evaluation of top-level lookups.
 */
public final class RuntimeStrSubstitutor extends StrSubstitutor {

    public RuntimeStrSubstitutor() {
    }

    public RuntimeStrSubstitutor(final Map<String, String> valueMap) {
        super(valueMap);
    }

    public RuntimeStrSubstitutor(final Properties properties) {
        super(properties);
    }

    public RuntimeStrSubstitutor(final StrLookup lookup) {
        super(lookup);
    }

    public RuntimeStrSubstitutor(final StrSubstitutor other) {
        super(other);
    }

    @Override
    boolean isRecursiveEvaluationAllowed() {
        return false;
    }

    @Override
    void setRecursiveEvaluationAllowed(final boolean recursiveEvaluationAllowed) {
        throw new UnsupportedOperationException(
                "recursiveEvaluationAllowed cannot be modified within RuntimeStrSubstitutor");
    }

    @Override
    public String toString() {
        return "RuntimeStrSubstitutor{" + super.toString() + "}";
    }
}
