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

package io.restassured.internal.http;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

/**
 * Strips credential-bearing headers (<code>Authorization</code>, <code>Cookie</code>, <code>Proxy-Authorization</code>)
 * from a request whose target host differs from the host of the original request in the same execution. This prevents a
 * redirect that crosses to a different origin from leaking the caller's token or session to that origin.
 * <p>
 * Apache HttpClient 4.5's request director copies the original request headers onto the redirected request after the
 * {@link org.apache.http.client.RedirectStrategy} runs, so the stripping has to happen on the outgoing request itself.
 * This interceptor runs for every request in the redirect chain and compares the current target host against the origin
 * host recorded on the first request in the same {@link HttpContext}. HttpClient 5.5 added equivalent behavior natively.
 */
public class CrossHostSensitiveHeaderStripper implements HttpRequestInterceptor {

    private static final String ORIGIN_HOST_ATTRIBUTE = "rest-assured.origin-host";
    private static final String[] SENSITIVE_HEADERS = {"Authorization", "Cookie", "Proxy-Authorization"};

    @Override
    public void process(HttpRequest request, HttpContext context) {
        HttpHost currentHost = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
        if (currentHost == null) {
            return;
        }

        HttpHost originHost = (HttpHost) context.getAttribute(ORIGIN_HOST_ATTRIBUTE);
        if (originHost == null) {
            // First request in this execution defines the origin; nothing to strip yet.
            context.setAttribute(ORIGIN_HOST_ATTRIBUTE, currentHost);
            return;
        }

        if (isCrossOrigin(originHost, currentHost)) {
            for (String header : SENSITIVE_HEADERS) {
                request.removeHeaders(header);
            }
        }
    }

    private static boolean isCrossOrigin(HttpHost origin, HttpHost current) {
        return !origin.getHostName().equalsIgnoreCase(current.getHostName())
                || !origin.getSchemeName().equalsIgnoreCase(current.getSchemeName())
                || effectivePort(origin) != effectivePort(current);
    }

    // HttpHost.getPort() is -1 when the URL omits the port; resolve it to the scheme default so that,
    // for example, http://example.com and http://example.com:80 are treated as the same origin.
    private static int effectivePort(HttpHost host) {
        int port = host.getPort();
        if (port != -1) {
            return port;
        }
        String scheme = host.getSchemeName();
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        return -1;
    }
}
