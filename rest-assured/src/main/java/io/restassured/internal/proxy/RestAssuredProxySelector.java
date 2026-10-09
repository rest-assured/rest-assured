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

package io.restassured.internal.proxy;

import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.specification.ProxySpecification;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;
import static java.net.Proxy.Type.HTTP;

/**
 * Proxy selector implementation that uses {@link ProxySpecification} to determine the Proxy to connect to.
 * If no ProxySpecification is defined then it delegates to the <code>delegatingProxySelector</code>.
 */
public class RestAssuredProxySelector extends ProxySelector {
    private ProxySelector delegatingProxySelector;
    private ProxySpecification proxySpecification;

    @Override
    public List<Proxy> select(URI uri) {
        notNull(uri, URI.class);
        if (proxySpecification != null) {
            List<Proxy> proxies = new ArrayList<>();
            proxies.add(new Proxy(HTTP, new InetSocketAddress(proxySpecification.getHost(), proxySpecification.getPort())));
            return proxies;
        }
        return delegatingProxySelector.select(uri);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        if (proxySpecification != null) {
            SafeExceptionRethrower.safeRethrow(ioe);
        } else {
            delegatingProxySelector.connectFailed(uri, sa, ioe);
        }
    }

    public ProxySelector getDelegatingProxySelector() {
        return delegatingProxySelector;
    }

    public void setDelegatingProxySelector(ProxySelector delegatingProxySelector) {
        this.delegatingProxySelector = delegatingProxySelector;
    }

    public ProxySpecification getProxySpecification() {
        return proxySpecification;
    }

    public void setProxySpecification(ProxySpecification proxySpecification) {
        this.proxySpecification = proxySpecification;
    }
}
