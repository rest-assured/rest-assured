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

import io.restassured.specification.ProxySpecification;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.protocol.HttpContext;

import java.net.ProxySelector;

/**
 * An implementation of ProxySelectorRoutePlanner that supports other schemes than http
 */
public class RestAssuredProxySelectorRoutePlanner extends ProxySelectorRoutePlanner {
    private final String scheme;

    /**
     * Creates a new proxy selector route planner.
     *
     * @param schemeRegistry the scheme registry
     * @param proxySelector  the proxy selector
     */
    public RestAssuredProxySelectorRoutePlanner(SchemeRegistry schemeRegistry, ProxySelector proxySelector, ProxySpecification proxySpecification) {
        super(schemeRegistry, proxySelector);
        this.scheme = proxySpecification == null ? null : proxySpecification.getScheme();
    }


    @Override
    protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        HttpHost httpHost = super.determineProxy(target, request, context);
        if (scheme != null && !scheme.equalsIgnoreCase(httpHost.getSchemeName())) {
            httpHost = new HttpHost(httpHost.getHostName(), httpHost.getPort(), scheme);
        }
        return httpHost;
    }
}
