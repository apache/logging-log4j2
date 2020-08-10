package org.apache.logging.log4j.core.lookup.spi;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.StrLookup;

public class AnolePropertiesLookup implements StrLookup {

    @Override
    public String lookup(String key) {
        return "this is an anole test";
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return "this is an anole test";
    }
}