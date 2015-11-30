package org.apache.logging.log4j.core.appender.routing;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.config.Scheduled;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * 
 * Policy is purging appenders that were not in use specified time in minutes
 *
 */
@Plugin(name = "IdlePurgePolicy", category = "Core", printObject = true)
@Scheduled
public class IdlePurgePolicy extends AbstractLifeCycle implements PurgePolicy, Runnable {

    private static final Logger LOGGER = StatusLogger.getLogger();
	private final long timeToLive;
	private final ConcurrentMap<String, Long> appendersUsage = new ConcurrentHashMap<>();
	private RoutingAppender routingAppender;
    private final ConfigurationScheduler scheduler;
    private volatile ScheduledFuture<?> future = null;
    
	public IdlePurgePolicy(long timeToLive, ConfigurationScheduler scheduler) {
		this.timeToLive = timeToLive;
        this.scheduler = scheduler;
	}	

    @Override
	public void initialize(RoutingAppender routingAppender) {
		this.routingAppender = routingAppender;
	}

    @Override
    public void stop() {
        super.stop();
        future.cancel(true);
    }

	/**
	 * Purging appenders that were not in use specified time
	 * 
	 */
	@Override
	public void purge() {
		long createTime = System.currentTimeMillis() - timeToLive;
    	for (Entry<String, Long> entry : appendersUsage.entrySet()) {
			if (entry.getValue() < createTime) {
                LOGGER.debug("Removing appender " + entry.getKey());
				appendersUsage.remove(entry.getKey());
		       	routingAppender.deleteAppender(entry.getKey());
			}
		}
	}

	@Override
	public void update(String key, LogEvent event) {
        long now = System.currentTimeMillis();
		appendersUsage.put(key, now);
        if (future == null) {
            synchronized(this) {
                if (future == null) {
                    scheduleNext();
                }
            }
        }

	}

    @Override
    public void run() {
        purge();
        scheduleNext();
    }

    private void scheduleNext() {
        long createTime = Long.MAX_VALUE;
        for (Entry<String, Long> entry : appendersUsage.entrySet()) {
            if (entry.getValue() < createTime) {
                createTime = entry.getValue();
            }
        }
        if (createTime < Long.MAX_VALUE) {
            long interval = timeToLive - (System.currentTimeMillis() - createTime);
            future = scheduler.schedule(this, interval, TimeUnit.MILLISECONDS);
        }
    }

	/**
     * Create the PurgePolicy
     * @param timeToLive the number of increments of timeUnit before the Appender should be purged.
     * @param timeUnit the unit of time the timeToLive is expressed in.
     * @return The Routes container.
     */
    @PluginFactory
    public static PurgePolicy createPurgePolicy(
            @PluginAttribute("timeToLive") final String timeToLive,
			@PluginAttribute("timeUnit") final String timeUnit,
            @PluginConfiguration Configuration configuration) {
    	
        if (timeToLive == null) {
            LOGGER.error("A timeToLive  value is required");
            return null;
        }
		TimeUnit units;
		if (timeUnit == null) {
			units = TimeUnit.MINUTES;
		} else {
			try {
				units = TimeUnit.valueOf(timeUnit.toUpperCase());
			} catch(Exception ex) {
				LOGGER.error("Invalid time unit {}", timeUnit);
				units = TimeUnit.MINUTES;
			}
		}
        
        final long ttl = units.toMillis(Long.parseLong(timeToLive));

        
        return new IdlePurgePolicy(ttl, configuration.getScheduler());
    }

    @Override
    public String toString() {
        return "timeToLive=" + timeToLive;
    }

}
