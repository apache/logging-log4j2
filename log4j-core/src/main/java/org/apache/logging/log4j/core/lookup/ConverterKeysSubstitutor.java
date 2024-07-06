package org.apache.logging.log4j.core.lookup;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

public class ConverterKeysSubstitutor {

  public static final String DEFAULT_ESCAPE = "%";

  public static final StrMatcher DEFAULT_PREFIX = StrMatcher.stringMatcher(DEFAULT_ESCAPE);
  public static final StrMatcher OPEN_BRACE_PREFIX = StrMatcher.stringMatcher("{");
  public static final StrMatcher CLOSE_BRACE_SUFFIX = StrMatcher.stringMatcher("}");

  private final StrSubstitutor strSubstitutor;
  private final Set<String> converterKeys = new HashSet<>();

  @SuppressWarnings("unchecked")
  public ConverterKeysSubstitutor(StrSubstitutor strSubstitutor) {

    this.strSubstitutor = strSubstitutor;

    final PluginManager manager = new PluginManager("Converter");
    manager.collectPlugins();
    final Map<String, PluginType<?>> plugins = manager.getPlugins();
    plugins.values().forEach(
        type -> {

          try {

            final Class<PatternConverter> clazz = (Class<PatternConverter>) type.getPluginClass();
            if (!LogEventPatternConverter.class.isAssignableFrom(clazz)) {
              return;
            }

            final ConverterKeys keys = clazz.getAnnotation(ConverterKeys.class);
            if (keys == null || keys.value() == null) {

              return;
            }

            this.converterKeys.addAll(
                Arrays.stream(keys.value())
                    .collect(Collectors.toSet())
            );

          } catch (final Exception ex) {

            // TODO throw ex
          }

        }
    );

  }

  public int findCloseBracePosition(
      final char[] chars,
      final int pos,
      final int bufEnd) {

    final StrMatcher prefixMatcher = this.strSubstitutor.getVariablePrefixMatcher();
    final int startMatchLen = DEFAULT_PREFIX.isMatch(chars, pos, pos, bufEnd);
    if (startMatchLen == 0) {

      return pos;
    }

    int openBraceCount = 0;
    int startKeyPos = pos + startMatchLen;
    int endKeyPos = startKeyPos;
    for (; endKeyPos < bufEnd; endKeyPos++) {

      final String convertKey = new String(chars, startKeyPos, endKeyPos - startKeyPos);

      if (!this.converterKeys.contains(convertKey)) {

        continue;
      }

      final boolean hasOpenBrace = prefixMatcher.isMatch(chars, endKeyPos, startKeyPos, bufEnd) == 0
          && OPEN_BRACE_PREFIX.isMatch(chars, endKeyPos, startKeyPos, bufEnd) != 0;
      if (!hasOpenBrace) {

        return endKeyPos;
      }

      openBraceCount++;
      if (openBraceCount > 0) {

        endKeyPos = this.findCloseBracePosition(chars, endKeyPos + 1, bufEnd);
      }

    }

    if (openBraceCount == 0) {

      return pos;
    }

    while (openBraceCount != 0 && endKeyPos > startKeyPos) {

      if (CLOSE_BRACE_SUFFIX.isMatch(chars, endKeyPos, startKeyPos, bufEnd) != 0) {

        openBraceCount--;
      }

      endKeyPos--;
    }

    // avoid close brace
    return endKeyPos + 1;
  }

}