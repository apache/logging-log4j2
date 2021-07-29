package org.apache.logging.log4j.core.lookup;

import java.util.Base64;

import org.apache.logging.log4j.core.LogEvent;

/**
 * Decodes Base64 strings.
 *
 * @since 3.0.0
 */
public class Base64StrLookup extends AbstractLookup {

    @Override
    public String lookup(final LogEvent event, final String key) {
        return new String(Base64.getDecoder().decode(key));
    }

}
