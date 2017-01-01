package org.apache.logging.log4j.core.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidPort;

/**
 * Plugin to hold a hostname and port (socket address).
 *
 * @since 2.8
 */
@Plugin(name = "SocketAddress", category = Node.CATEGORY, printObject = true)
public class SocketAddress {

    /**
     * Creates a SocketAddress corresponding to {@code localhost:0}.
     *
     * @return a SocketAddress for {@code localhost:0}
     */
    public static SocketAddress getLoopback() {
        return new SocketAddress(InetAddress.getLoopbackAddress(), 0);
    }

    private final InetSocketAddress socketAddress;

    private SocketAddress(final InetAddress host, final int port) {
        this.socketAddress = new InetSocketAddress(host, port);
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public int getPort() {
        return socketAddress.getPort();
    }

    public InetAddress getAddress() {
        return socketAddress.getAddress();
    }

    public String getHostName() {
        return socketAddress.getHostName();
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<SocketAddress> {

        @PluginBuilderAttribute
        @ValidHost
        private InetAddress host;

        @PluginBuilderAttribute
        @ValidPort
        private int port;

        public Builder setHost(final InetAddress host) {
            this.host = host;
            return this;
        }

        public Builder setPort(final int port) {
            this.port = port;
            return this;
        }

        @Override
        public SocketAddress build() {
            return new SocketAddress(host, port);
        }
    }

}
