/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.specification;

import io.restassured.internal.common.assertion.AssertParameter;
import org.apache.commons.lang3.StringUtils;

/**
 * A proxy specification that defines a hostname, port and scheme for the proxy.
 */
public class ProxySpecification {

    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final String DEFAULT_SCHEME = HTTP;
    private static final String DEFAULT_USERNAME = null;
    private static final String DEFAULT_PASSWORD = null;
    private static final int DEFAULT_PORT = 8888;
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;


    private final String host;
    private final int port;
    private final String scheme;
    private final String username;
    private final String password;

    /**
     * Creates a ProxySpecification with the supplied hostname, port and scheme.
     *
     * @param host   The hostname of the proxy.
     * @param port   The port of the proxy.
     * @param scheme The scheme of the proxy.
     */
    public ProxySpecification(String host, int port, String scheme) {
        this(host, port, scheme, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    private ProxySpecification(String host, int port, String scheme, String username, String password) {
        this.host = StringUtils.trimToNull(host);
        this.scheme = StringUtils.trimToNull(scheme);
        AssertParameter.notNull(this.host, "Proxy host");
        AssertParameter.notNull(this.scheme, "Proxy scheme");
        if (port < 1) {
            if (scheme.equalsIgnoreCase(HTTP)) {
                port = DEFAULT_HTTP_PORT;
            } else if (scheme.equalsIgnoreCase(HTTPS)) {
                port = DEFAULT_HTTPS_PORT;
            } else {
                throw new IllegalArgumentException("Cannot determine proxy port");
            }
        }
        this.username = StringUtils.trimToNull(username);
        this.password = StringUtils.trimToNull(password);
        this.port = port;
    }

    /**
     * Specify the hostname for of the proxy. Will use port {@value #DEFAULT_PORT} and scheme {@value #DEFAULT_SCHEME}.
     *
     * @param host The hostname
     * @return A new ProxySpecification instance
     */
    public static ProxySpecification host(String host) {
        return new ProxySpecification(host, DEFAULT_PORT, DEFAULT_SCHEME, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    /**
     * Specify the port of the proxy. Will use hostname {@value #DEFAULT_HOST} and scheme {@value #DEFAULT_SCHEME}.
     *
     * @param port The port on localhost to connect to.
     * @return A new ProxySpecification instance
     */
    public static ProxySpecification port(int port) {
        return new ProxySpecification(DEFAULT_HOST, port, DEFAULT_SCHEME, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    /**
     * Specify preemptive basic authentication for the proxy. Will use hostname {@value #DEFAULT_HOST}, port {@value #DEFAULT_PORT} and scheme {@value #DEFAULT_SCHEME}.
     *
     * @param username The username
     * @param password The username
     * @return A new ProxySpecification instance
     */
    public static ProxySpecification auth(String username, String password) {
        AssertParameter.notNull(username, "username");
        AssertParameter.notNull(password, "password");
        return new ProxySpecification(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_SCHEME, username, password);
    }

    /**
     * Specify the port of the proxy.
     *
     * @param port The port of the proxy.
     * @return A new ProxySpecification instance
     */
    public ProxySpecification withPort(int port) {
        return new ProxySpecification(host, port, scheme, username, password);
    }

    /**
     * Specify the hostname of the proxy.
     *
     * @param host The hostname of the proxy.
     * @return A new ProxySpecification instance
     */
    public ProxySpecification withHost(String host) {
        return new ProxySpecification(host, port, scheme, username, password);
    }

    /**
     * Specify (preemptive) basic authentication for the proxy
     *
     * @param username The username
     * @param password The username
     * @return A new ProxySpecification instance
     */
    public ProxySpecification withAuth(String username, String password) {
        AssertParameter.notNull(username, "username");
        AssertParameter.notNull(password, "password");
        return new ProxySpecification(host, port, scheme, username, password);
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

    /**
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    boolean hasAuth() {
        return username != null || password != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProxySpecification)) return false;

        ProxySpecification that = (ProxySpecification) o;

        if (port != that.port) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (scheme != null ? !scheme.equals(that.scheme) : that.scheme != null) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        return !(password != null ? !password.equals(that.password) : that.password != null);

    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + (scheme != null ? scheme.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return scheme + "://" + host + ":" + port;
    }
}
