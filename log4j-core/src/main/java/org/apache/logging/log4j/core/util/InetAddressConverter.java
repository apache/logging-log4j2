package org.apache.logging.log4j.core.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.beust.jcommander.IStringConverter;

public class InetAddressConverter implements IStringConverter<InetAddress> {

    @Override
    public InetAddress convert(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(host, e);
        }
    }

}