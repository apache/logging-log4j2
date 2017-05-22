package org.apache.logging.log4j.core.appender.rewrite;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusLogger;

@Plugin(name = "RegexRewritePolicy", category = "Core", elementType = "rewritePolicy", printObject = true)
public class RegexRewritePolicy implements RewritePolicy {

  protected static final Logger LOGGER = StatusLogger.getLogger();

  private Map<String, String> _patternMap;

  public RegexRewritePolicy(final Map<String, String> map) {
    _patternMap = map;
  }

  @Override
  public LogEvent rewrite(LogEvent source) {
    Message message = source.getMessage();
    if (message instanceof SimpleMessage) {
      SimpleMessage sm = (SimpleMessage) message;
      String formattedMessage = sm.getFormattedMessage();
      for (Map.Entry<String, String> e : _patternMap.entrySet()) {
        formattedMessage = formattedMessage.replaceAll(e.getKey(), e.getValue());
      }
      return new Log4jLogEvent.Builder(source).setMessage(new SimpleMessage(formattedMessage)).build();
    }
    return source;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(" {");
    boolean first = true;
    for (final Map.Entry<String, String> entry : _patternMap.entrySet()) {
      if (!first) {
        sb.append(", ");
      }
      sb.append(entry.getKey()).append('=').append(entry.getValue());
      first = false;
    }
    sb.append('}');
    return sb.toString();
  }

  /**
   * The factory method to create the RegexRewritePolicy.
   * 
   * @param pairs key/value pairs for the match regex and replace
   * @return The MapRewritePolicy.
   */
  @PluginFactory
  public static RegexRewritePolicy createPolicy(@PluginElement("KeyValuePair") final KeyValuePair[] pairs) {
    if (pairs == null || pairs.length == 0) {
      LOGGER.error("At least one KeyValuePair must be specified for the RegexRewritePolicy");
      return null;
    }
    final Map<String, String> map = new HashMap<String, String>();
    for (final KeyValuePair pair : pairs) {
      final String key = pair.getKey();
      if (key == null) {
        LOGGER.error("A null key is not valid in RegexRewritePolicy");
        continue;
      }
      final String value = pair.getValue();
      if (value == null) {
        LOGGER.error("A null value for key " + key + " is not allowed in RegexRewritePolicy");
        continue;
      }
      map.put(pair.getKey(), pair.getValue());
    }
    if (map.isEmpty()) {
      LOGGER.error("RegexRewritePolicy is not configured with any valid key value pairs");
      return null;
    }
    return new RegexRewritePolicy(map);
  }
}
