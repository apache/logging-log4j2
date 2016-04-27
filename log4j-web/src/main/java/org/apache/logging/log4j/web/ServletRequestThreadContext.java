package org.apache.logging.log4j.web;

import java.util.Objects;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.ThreadContext;

public class ServletRequestThreadContext {

    public static void put(String key, ServletRequest servletRequest) {
        put(key, "RemoteAddr", servletRequest.getRemoteAddr());
        put(key, "RemoteHost", servletRequest.getRemoteHost());
        put(key, "RemotePort", servletRequest.getRemotePort());
    }

    public static void put(String key, String field, Object value) {
        put(key + "." + field, Objects.toString(value));
    }

    public static void put(String key, String value) {
        ThreadContext.put(key, value);
    }

    public static void put(String key, HttpServletRequest servletRequest) {
        put(key, (ServletRequest) servletRequest);
    }
}
