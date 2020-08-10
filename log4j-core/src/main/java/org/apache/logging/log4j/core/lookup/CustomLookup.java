package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertySource;

import java.util.*;

/**
 * Looks up keys from sources specified by users. By default the custom source list is empty, but users
 * may specify some custom classes as lookup sources via SPI.
 */
@Plugin(name = "custom", category = StrLookup.CATEGORY)
public class CustomLookup implements StrLookup{

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Custom lookup sources.
     */
    private final Set<StrLookup> customSources;

    public CustomLookup(){
        this.customSources = new TreeSet<>(new Comparator<StrLookup>(){
            @Override
            public int compare(StrLookup o1, StrLookup o2) {
                return o1.getClass().getName().compareTo(o2.getClass().getName());
            }
        });
        for (final ClassLoader classLoader : LoaderUtil.getClassLoaders()) {
            try {
                for (final StrLookup lookupSource : ServiceLoader.load(StrLookup.class, classLoader)) {
                    customSources.add(lookupSource);
                }
            } catch (final Throwable ex) {
                LOGGER.warn("There is something wrong occurred in custom lookup initialization. Details: {}", ex.getMessage());
            }
        }
    }

    @Override
    public String lookup(String key) {
        for(StrLookup strLookup : customSources){
            String tempResult = strLookup.lookup(key);
            if(tempResult != null){
                return tempResult;
            }
        }
        return null;
    }

    @Override
    public String lookup(LogEvent event, String key) {
        for(StrLookup strLookup : customSources){
            String tempResult = strLookup.lookup(event, key);
            if(tempResult != null){
                return tempResult;
            }
        }
        return null;
    }
}
