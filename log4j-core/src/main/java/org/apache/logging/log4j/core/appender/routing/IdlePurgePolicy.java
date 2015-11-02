package org.apache.logging.log4j.core.appender.routing;

import java.util.Calendar;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * 
 * Policy is purging appenders that were not in use specified time in minutes
 *
 */
@Plugin(name = "IdlePurgePolicy", category = "Core", printObject = true)
public class IdlePurgePolicy implements PurgePolicy {

    private static final Logger LOGGER = StatusLogger.getLogger();
	private int timeToLive;
	private final ConcurrentMap<String, Calendar> appendersUsage = new ConcurrentHashMap<>();
	private RoutingAppender routingAppender;
    
	public IdlePurgePolicy(int timeToLive) {
		this.timeToLive = timeToLive;		
	}	

    @Override
	public void initialize(RoutingAppender routingAppender) {
		this.routingAppender = routingAppender;		
	}

	/**
	 * Purging appenders that were not in use specified time
	 * 
	 */
	@Override
	public void purge() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -timeToLive);
		
    	for (Entry<String, Calendar> entry : appendersUsage.entrySet()) {
			if(calendar.after(entry.getValue())) {				
				appendersUsage.remove(entry.getKey());
		       	routingAppender.deleteAppender(entry.getKey());
			}
		}
	}

	@Override
	public void update(String key, LogEvent event) {
		appendersUsage.put(key, Calendar.getInstance());
	}

	/**
     * Create the Routes.
     * @param pattern The pattern.
     * @param routes An array of Route elements.
     * @return The Routes container.
     */
    @PluginFactory
    public static PurgePolicy createPurgePolicy(
            @PluginAttribute("timetolive") final String time) {
    	
        if (time == null) {
            LOGGER.error("A timeToLive is required");
            return null;
        }
        
        final int timeToLive = Integers.parseInt(time);
        
        return new IdlePurgePolicy(timeToLive);
    }

}
