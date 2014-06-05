/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jayway.restassured.specification;

import org.apache.commons.lang3.StringUtils;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;

/**
 * A proxy specification that defines a hostname, port and scheme for the proxy.
 */
public class ProxySpecification {

    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final String DEFAULT_SCHEME = HTTP;
    private static final int DEFAULT_PORT = 8888;
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;


    private final String host;
    private final int port;
    private final String scheme;

    /**
     * Creates a ProxySpecification with the supplied hostname, port and scheme.
     *
     * @param host   The hostname of the proxy.
     * @param port   The port of the proxy.
     * @param scheme The scheme of the proxy.
     */
    public ProxySpecification(String host, int port, String scheme) {
        this.host = StringUtils.trimToNull(host);
        this.scheme = StringUtils.trimToNull(scheme);
        notNull(this.host, "Proxy host");
        notNull(this.scheme, "Proxy scheme");
        if (port < 1) {
            if (scheme.equalsIgnoreCase(HTTP)) {
                port = DEFAULT_HTTP_PORT;
            } else if (scheme.equalsIgnoreCase(HTTPS)) {
                port = DEFAULT_HTTPS_PORT;
            } else {
                throw new IllegalArgumentException("Cannot determine proxy port");
            }
        }
        this.port = port;
    }

    /**
     * Specify the hostname for of the proxy. Will use port {@value #DEFAULT_PORT} and scheme {@value #DEFAULT_SCHEME}.
     *
     * @param host The hostname
     * @return A new ProxySpecification instance
     */
    public static ProxySpecification host(String host) {
        return new ProxySpecification(host, DEFAULT_PORT, DEFAULT_SCHEME);
    }

    /**
     * Specify the port for of the proxy. Will use hostname {@value #DEFAULT_HOST} and scheme {@value #DEFAULT_SCHEME}.
     *
     * @param port The port on localhost to connect to.
     * @return A new ProxySpecification instance
     */
    public static ProxySpecification port(int port) {
        return new ProxySpecification(DEFAULT_HOST, port, DEFAULT_SCHEME);
    }

    /**
     * Specify the port of the proxy.
     *
     * @param port The port of the proxy.
     * @return A new ProxySpecification instance
     */
    public ProxySpecification withPort(int port) {
        return new ProxySpecification(host, port, scheme);
    }

    /**
     * Specify the hostname of the proxy.
     *
     * @param host The hostname of the proxy.
     * @return A new ProxySpecification instance
     */
    public ProxySpecification withHost(String host) {
        return new ProxySpecification(host, port, scheme);
    }

    /**
     * Specify the scheme of the proxy.
     *
     * @param scheme The scheme of the proxy.
     * @return A new ProxySpecification instance
     */
    public ProxySpecification withScheme(String scheme) {
        return new ProxySpecification(host, port, scheme);
    }

    /**
     * Syntactic sugar.
     *
     * @return The same ProxySpecification instance
     */
    public ProxySpecification and() {
        return this;
    }

    /**
     * @return The hostname
     */
    public String getHost() {
        return host;
    }

    /**
     * @return The port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return The scheme
     */
    public String getScheme() {
        return scheme;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProxySpecification that = (ProxySpecification) o;

        if (port != that.port) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (scheme != null ? !scheme.equals(that.scheme) : that.scheme != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + (scheme != null ? scheme.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return scheme + "://" + host + ":" + port;
    }
}
