package org.apache.logging.log4j.core.lookup.spi;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.plugins.Plugin;

@Plugin(name="", category = StrLookup.CATEGORY)
public class AnolePropertiesLookup implements StrLookup {

    @Override
    public String lookup(String key) {
        if("anoleKey".equals(key)){
            return "hello anole";
        }
        return null;
    }

    @Override
    public String lookup(LogEvent event, String key) {
        if("anoleKey".equals(key)){
            return "hello anole";
        }
        return null;
    }
}