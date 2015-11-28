package org.apache.logging.log4j.core.appender.routing;

import org.apache.logging.log4j.core.LogEvent;

/**
 * 
 * Policy for purging routed appenders
 *
 */
public interface PurgePolicy {

	/**
	 * Activate purging appenders
	 */
	void purge();
	
	/**
	 * 
	 * @param routed appender key
	 * @param event
	 */
	void update(String key, LogEvent event);

	/**
	 * Initialize with routing appender
	 * 
	 * @param routingAppender
	 */
	void initialize(RoutingAppender routingAppender);

}
