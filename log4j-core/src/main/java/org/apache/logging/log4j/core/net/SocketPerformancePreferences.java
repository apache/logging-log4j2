package org.apache.logging.log4j.core.net;

import java.net.Socket;
import java.net.SocketException;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.util.Builder;

/**
 * Holds all socket options settable via {@link Socket#setPerformancePreferences(int, int, int)}.
 * <p>
 * The {@link Socket#setPerformancePreferences(int, int, int)} API may not be implemented by a JRE.
 * </p>
 */
@Plugin(name = "SocketPerformancePreferences", category = Core.CATEGORY_NAME, printObject = true)
public class SocketPerformancePreferences implements Builder<SocketPerformancePreferences>, Cloneable {

    @PluginBuilderFactory
    public static SocketPerformancePreferences newBuilder() {
        return new SocketPerformancePreferences();
    }

    @PluginBuilderAttribute
    @Required
    private int bandwidth;

    @PluginBuilderAttribute
    @Required
    private int connectionTime;

    @PluginBuilderAttribute
    @Required
    private int latency;

    public void apply(final Socket socket) {
        socket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public SocketPerformancePreferences build() {
        try {
            return (SocketPerformancePreferences) clone();
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public int getConnectionTime() {
        return connectionTime;
    }

    public int getLatency() {
        return latency;
    }

    public void setBandwidth(final int bandwidth) {
        this.bandwidth = bandwidth;
    }

    public void setConnectionTime(final int connectionTime) {
        this.connectionTime = connectionTime;
    }

    public void setLatency(final int latency) {
        this.latency = latency;
    }

    @Override
    public String toString() {
        return "SocketPerformancePreferences [bandwidth=" + bandwidth + ", connectionTime=" + connectionTime
                + ", latency=" + latency + "]";
    }

}
