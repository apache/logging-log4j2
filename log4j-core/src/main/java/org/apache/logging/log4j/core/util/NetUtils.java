/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.util;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.internal.annotation.SuppressFBWarnings;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Networking-related convenience methods.
 */
public final class NetUtils {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String UNKNOWN_LOCALHOST = "UNKNOWN_LOCALHOST";

    private NetUtils() {
        // empty
    }

    /**
     * This method gets the network name of the machine we are running on. Returns "UNKNOWN_LOCALHOST" in the unlikely
     * case where the host name cannot be found.
     *
     * @return String the name of the local host
     */
    public static String getLocalHostname() {
        return getHostname(InetAddress::getHostName);
    }

    /**
     * This method gets the FQDN of the machine we are running on.
     * It returns {@value UNKNOWN_LOCALHOST} if the host name cannot be found.
     *
     * @return The canonical name of the local host; or {@value UNKNOWN_LOCALHOST}, if cannot be found.
     */
    public static String getCanonicalLocalHostname() {
        return getHostname(InetAddress::getCanonicalHostName);
    }

    private static String getHostname(final Function<? super InetAddress, ? extends String> callback) {
        try {
            final InetAddress address = InetAddress.getLocalHost();
            return address == null ? UNKNOWN_LOCALHOST : callback.apply(address);
        } catch (final UnknownHostException uhe) {
            try {
                final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    final NetworkInterface nic = interfaces.nextElement();
                    final Enumeration<InetAddress> addresses = nic.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        final InetAddress address = addresses.nextElement();
                        if (!address.isLoopbackAddress()) {
                            final String hostname = callback.apply(address);
                            if (hostname != null) {
                                return hostname;
                            }
                        }
                    }
                }
            } catch (final SocketException se) {
                // ignore and log below.
            }
            LOGGER.error("Could not determine local host name", uhe);
            return UNKNOWN_LOCALHOST;
        }
    }

    /**
     *  Returns the local network interface's MAC address if possible. The local network interface is defined here as
     *  the {@link java.net.NetworkInterface} that is both up and not a loopback interface.
     *
     * @return the MAC address of the local network interface or {@code null} if no MAC address could be determined.
     */
    public static byte[] getMacAddress() {
        byte[] mac = null;
        try {
            final InetAddress localHost = InetAddress.getLocalHost();
            try {
                final NetworkInterface localInterface = NetworkInterface.getByInetAddress(localHost);
                if (isUpAndNotLoopback(localInterface)) {
                    mac = localInterface.getHardwareAddress();
                }
                if (mac == null) {
                    final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                    if (networkInterfaces != null) {
                        while (networkInterfaces.hasMoreElements() && mac == null) {
                            final NetworkInterface nic = networkInterfaces.nextElement();
                            if (isUpAndNotLoopback(nic)) {
                                mac = nic.getHardwareAddress();
                            }
                        }
                    }
                }
            } catch (final SocketException e) {
                LOGGER.catching(e);
            }
            if (ArrayUtils.isEmpty(mac) && localHost != null) {
                // Emulate a MAC address with an IP v4 or v6
                final byte[] address = localHost.getAddress();
                // Take only 6 bytes if the address is an IPv6 otherwise will pad with two zero bytes
                mac = Arrays.copyOf(address, 6);
            }
        } catch (final UnknownHostException ignored) {
            // ignored
        }
        return mac;
    }

    /**
     * Returns the mac address, if it is available, as a string with each byte separated by a ":" character.
     * @return the mac address String or null.
     */
    public static String getMacAddressString() {
        final byte[] macAddr = getMacAddress();
        if (!ArrayUtils.isEmpty(macAddr)) {
            final StringBuilder sb = new StringBuilder(String.format("%02x", macAddr[0]));
            for (int i = 1; i < macAddr.length; ++i) {
                sb.append(":").append(String.format("%02x", macAddr[i]));
            }
            return sb.toString();
        }
        return null;
    }

    private static boolean isUpAndNotLoopback(final NetworkInterface ni) throws SocketException {
        return ni != null && !ni.isLoopback() && ni.isUp();
    }

    /**
     * Converts a configuration location, expressed as either a {@link URI} or a file system path, into a {@link URI}.
     *
     * <p>The argument is classified by syntax:</p>
     *
     * <ul>
     * <li>An <strong>absolute URI</strong> (one bearing a scheme) is returned unchanged.
     * A single-letter scheme is the exception: it is read as a Windows drive letter, not a URI scheme.</li>
     * <li>An <strong>absolute file system path</strong> for the current OS is converted to a {@code file:} URI.</li>
     * <li>A <strong>relative</strong> path or URI ({@code log4j2.xml}, {@code config/log4j2.xml}) is returned as a
     * relative URI and will require disambiguation later.</li>
     * </ul>
     *
     * <p><em>Note:</em> URI parsing is performed leniently and might fix minor syntax errors.</p>
     *
     * @param path a URI string or a file system path
     * @return the matching URI, never {@code null}
     */
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "Currently `path` comes from a configuration file.")
    public static URI toURI(final String path) {
        final URI uri = parseURI(path);
        // 1. A genuine absolute URI (a real scheme, not a Windows drive letter) is used as commanded.
        if (uri != null && uri.isAbsolute() && !isWindowsDriveLetter(uri.getScheme())) {
            return uri;
        }
        // 2. Otherwise, the value is a file system path:
        //
        // - an absolute path becomes a `file:` URI holding the full path,
        // - a relative one becomes a scheme-less URI, to be resolved later as a file or a class path resource.
        final File file = new File(path);
        return file.isAbsolute() ? file.toURI() : toRelativeUri(file);
    }

    /**
     * Converts a relative file system path into a scheme-less {@link URI}.
     *
     * <p>Characters illegal in a URI (such as {@code \} or blanks) are percent-encoded.
     * A leading Windows drive letter is escaped too ({@code C:} becomes {@code C%3A}),
     * so the result is a relative reference rather than a URI whose scheme is the drive letter.</p>
     */
    private static URI toRelativeUri(final File file) {
        try {
            final URI uri = new URI(null, null, file.toString(), null);
            return uri.isAbsolute() ? URI.create(uri.toASCIIString().replaceFirst(":", "%3A")) : uri;
        } catch (final URISyntaxException e) {
            // Unreachable: the multi-argument constructor escapes every character illegal in a URI.
            throw new IllegalArgumentException("Cannot convert to a relative URI: " + file, e);
        }
    }

    /**
     * Leniently parses a string into a {@link URI}.
     *
     * <p>The string is first parsed with {@link URI#URI(String)}. If it is not valid URI syntax,
     * it is parsed with {@link URL#URL(String)} and rebuilt into a properly encoded URI.</p>
     *
     * <p>The {@link URL} step consults the installed {@link java.net.URLStreamHandler URL stream handlers},
     * so a scheme contributed by Apache Commons VFS (or any other {@link java.net.URLStreamHandlerFactory}) is
     * recognized and its illegal characters are encoded.</p>
     *
     * @param value the value to parse
     * @return the parsed {@link URI}, or {@code null} if neither {@link URI} nor {@link URL} can parse it
     */
    private static /*@Nullable*/ URI parseURI(final String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            LOGGER.trace("Could not parse value '{}' as URI. Falling back to URL parsing.", value, e);
        }
        try {
            final URL url = new URL(value);
            return new URI(
                    url.getProtocol(),
                    url.getUserInfo(),
                    url.getHost(),
                    url.getPort(),
                    url.getPath(),
                    url.getQuery(),
                    null);
        } catch (MalformedURLException | URISyntaxException e) {
            LOGGER.trace("Could not parse value '{}' as URL.", value, e);
        }
        return null;
    }

    /**
     * Returns {@code true} if the given URI scheme is in fact a Windows drive letter, that is, a single letter.
     *
     * <p>No registered URI scheme is a single letter, so a single-letter scheme always denotes the drive letter of a
     * path such as {@code C:/dir/file} that happens to be valid URI syntax.</p>
     */
    private static boolean isWindowsDriveLetter(final /* @Nullable */ String scheme) {
        return scheme != null && scheme.length() == 1 && Character.isLetter(scheme.charAt(0));
    }

    public static List<URI> toURIs(final String path) {
        final String[] parts = path.split(",");
        String scheme = null;
        final List<URI> uris = new ArrayList<>(parts.length);
        for (final String part : parts) {
            final URI uri = NetUtils.toURI(scheme != null ? scheme + ":" + part.trim() : part.trim());
            if (scheme == null && uri.getScheme() != null) {
                scheme = uri.getScheme();
            }
            uris.add(uri);
        }
        return uris;
    }
}
