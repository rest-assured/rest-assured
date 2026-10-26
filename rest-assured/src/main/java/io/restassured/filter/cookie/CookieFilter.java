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

package io.restassured.filter.cookie;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.Cookies;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.internal.http.cookie.CookieOrigin;
import io.restassured.internal.http.cookie.CookieSpec;
import io.restassured.internal.http.cookie.CookieStore;
import io.restassured.internal.http.cookie.Rfc6265CookieSpec;
import io.restassured.internal.http.cookie.StoredCookie;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.http.HttpHost;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HttpContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * The cookie filter can be used to keep track of all the cookies sent by the server and use them in subsequent requests.
 * It matches the cookies to the request URL within the rules of RFC 6265.
 * It might come in handy when more than just the {@link SessionFilter} and browser-like cookie matching is needed.
 * For example:
 * <pre>
 * CookieFilter cookieFilter = new CookieFilter();
 *
 * given().
 *         filter(cookieFilter).
 * expect().
 *         statusCode(200).
 * when().
 *         get("/x");
 *
 * given().
 *         cookie("foo", "bar").
 *         filter(cookieFilter). // Reuse the same cookie filter
 *                      // if "foo" is stored in cookieFilter it won't be applied because it's already applied explicitly
 * expect().
 *         statusCode(200).
 * when().
 *         get("/y");
 * </pre>
 */
public class CookieFilter implements Filter {
    private final boolean allowMultipleCookiesWithTheSameName;
    private final CookieSpec cookieSpec;
    private final CookieStore cookieStore;

    /**
     * Create an instance of {@link CookieFilter} that will prevent cookies with the same name to be sent twice.
     *
     * @see CookieFilter#CookieFilter(boolean)
     */
    public CookieFilter() {
        this(false);
    }

    /**
     * Create an instance of {@link CookieFilter} that allows specifying whether or not it should accept (and thus send) multiple cookies with the same name.
     * Default is <code>false</code>.
     *
     * @param allowMultipleCookiesWithTheSameName Specify whether or not to allow found two cookies with same name eg. JSESSIONID with different paths.
     */
    public CookieFilter(boolean allowMultipleCookiesWithTheSameName) {
        this.allowMultipleCookiesWithTheSameName = allowMultipleCookiesWithTheSameName;
        this.cookieSpec = new Rfc6265CookieSpec();
        this.cookieStore = new CookieStore();
    }

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        CookieOrigin cookieOrigin = cookieOriginFromUri(requestSpec.getURI());
        for (StoredCookie cookie : cookieStore.match(cookieSpec, cookieOrigin)) {
            if (allowMultipleCookiesWithTheSameNameOrCookieNotPreviouslyDefined(requestSpec, cookie)) {
                requestSpec.cookie(cookie.getName(), cookie.getValue());
            }
        }

        final Response response = ctx.next(requestSpec, responseSpec);
        storeCookiesFromResponseIfApplicable(cookieOrigin, response);
        return response;
    }

    public CookieFilter storeCookiesFromResponseIfApplicable(String requestUri, Response response) {
        CookieOrigin cookieOrigin = cookieOriginFromUri(requestUri);
        storeCookiesFromResponseIfApplicable(cookieOrigin, response);
        return this;
    }


    private void storeCookiesFromResponseIfApplicable(CookieOrigin cookieOrigin, Response response) {
        // When the server responds with a redirect (e.g., 302), REST Assured follows it and issues a new request.
        // However, its request URI (requestSpec.getURI()) still reflects the original URI, not the final one that set
        // the cookies. Since cookies are path- and domain-scoped, we must use the *effective* URI of the final response
        // (after redirects). Otherwise, valid Set-Cookie headers might be rejected due to a mismatched domain or path.
        if (response instanceof RestAssuredResponseImpl) {
            URI effectiveUri = ((RestAssuredResponseImpl) response).getEffectiveUri();
            if (effectiveUri != null) {
                cookieOrigin = CookieOrigin.of(effectiveUri);
            }
        }

        for (String cookieValue : response.getHeaders().getValues("Set-Cookie")) {
            StoredCookie cookie = cookieSpec.parse(cookieValue, cookieOrigin);
            if (cookie != null) {
                cookieStore.add(cookie);
            }
        }
    }

    private boolean allowMultipleCookiesWithTheSameNameOrCookieNotPreviouslyDefined(FilterableRequestSpecification requestSpec, StoredCookie cookie) {
        return allowMultipleCookiesWithTheSameName || !requestSpec.getCookies().hasCookieWithName(cookie.getName());
    }

    private CookieOrigin cookieOriginFromUri(String uri) {
        try {
            return CookieOrigin.of(new URI(uri));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * @return The cookies this filter has stored, as an HttpClient 4 cookie store, which adds to and removes from them
     * @deprecated HttpClient 4 types are removed in REST Assured 7.0.0. Use {@link #getCookies()} and
     * {@link #addCookie(io.restassured.http.Cookie)} instead.
     */
    @Deprecated
    public org.apache.http.client.CookieStore getCookieStore() {
        return new org.apache.http.client.CookieStore() {
            @Override
            public void addCookie(org.apache.http.cookie.Cookie cookie) {
                if (cookie != null) {
                    cookieStore.add(toStoredCookie(cookie));
                }
            }

            @Override
            public List<org.apache.http.cookie.Cookie> getCookies() {
                List<org.apache.http.cookie.Cookie> cookies = new ArrayList<>();
                for (StoredCookie cookie : cookieStore.cookies()) {
                    cookies.add(toClientCookie(cookie));
                }
                return cookies;
            }

            @Override
            public boolean clearExpired(Date date) {
                return date != null && cookieStore.clearExpired(date);
            }

            @Override
            public void clear() {
                cookieStore.clear();
            }
        };
    }

    private static BasicClientCookie toClientCookie(StoredCookie cookie) {
        BasicClientCookie clientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
        clientCookie.setDomain(cookie.getDomain());
        clientCookie.setPath(cookie.getPath());
        clientCookie.setSecure(cookie.isSecure());
        clientCookie.setComment(cookie.getComment());
        clientCookie.setExpiryDate(cookie.getExpiryDate());
        clientCookie.setVersion(cookie.getVersion());
        if (cookie.hasDomainAttribute()) {
            clientCookie.setAttribute(ClientCookie.DOMAIN_ATTR, cookie.getDomain());
        }
        if (cookie.isHttpOnly()) {
            clientCookie.setAttribute("httponly", "");
        }
        return clientCookie;
    }

    private static StoredCookie toStoredCookie(org.apache.http.cookie.Cookie cookie) {
        StoredCookie storedCookie = new StoredCookie(cookie.getName(), cookie.getValue());
        ClientCookie clientCookie = cookie instanceof ClientCookie ? (ClientCookie) cookie : null;
        if (clientCookie != null && clientCookie.containsAttribute(ClientCookie.DOMAIN_ATTR)) {
            storedCookie.setDomainAttribute(cookie.getDomain());
        } else {
            storedCookie.setDomain(cookie.getDomain());
        }
        storedCookie.setPath(cookie.getPath());
        storedCookie.setSecure(cookie.isSecure());
        storedCookie.setComment(cookie.getComment());
        storedCookie.setExpiryDate(cookie.getExpiryDate());
        storedCookie.setVersion(cookie.getVersion());
        storedCookie.setHttpOnly(clientCookie != null && clientCookie.containsAttribute("httponly"));
        return storedCookie;
    }

    /**
     * @return A snapshot of the cookies this filter has stored. Expired cookies are left out.
     */
    public Cookies getCookies() {
        List<io.restassured.http.Cookie> cookies = new ArrayList<>();
        Date now = new Date();
        for (StoredCookie cookie : cookieStore.cookies()) {
            if (cookie.isExpired(now)) {
                continue;
            }
            io.restassured.http.Cookie.Builder builder = new io.restassured.http.Cookie.Builder(cookie.getName(), cookie.getValue())
                    .setSecured(cookie.isSecure())
                    .setVersion(cookie.getVersion());
            if (cookie.getDomain() != null) {
                builder.setDomain(cookie.getDomain());
            }
            if (cookie.getPath() != null) {
                builder.setPath(cookie.getPath());
            }
            if (cookie.getExpiryDate() != null) {
                builder.setExpiryDate(cookie.getExpiryDate());
            }
            if (cookie.getComment() != null) {
                builder.setComment(cookie.getComment());
            }
            if (cookie.isHttpOnly()) {
                builder.setHttpOnly(true);
            }
            cookies.add(builder.build());
        }
        return new Cookies(cookies);
    }

    /**
     * Store a cookie in this filter, so that it's sent in the requests it matches, as if a response had set it.
     * The cookie must have a domain, and it's sent only to that host. Without a path it's sent for every path.
     * It replaces a stored cookie with the same name, domain and path.
     *
     * @param cookie The cookie to store.
     * @return This CookieFilter
     */
    public CookieFilter addCookie(io.restassured.http.Cookie cookie) {
        notNull(cookie, "cookie");
        if (!cookie.hasDomain()) {
            throw new IllegalArgumentException("Cookie '" + cookie.getName() + "' needs a domain to be added to a CookieFilter");
        }
        StoredCookie storedCookie = new StoredCookie(cookie.getName(), cookie.getValue());
        storedCookie.setDomain(cookie.getDomain());
        storedCookie.setPath(cookie.hasPath() ? cookie.getPath() : "/");
        storedCookie.setSecure(cookie.isSecured());
        if (cookie.hasExpiryDate()) {
            storedCookie.setExpiryDate(cookie.getExpiryDate());
        } else if (cookie.hasMaxAge() && cookie.getMaxAge() >= 0) {
            // Like a Max-Age attribute in a response, which REST Assured turns into an expiry date
            storedCookie.setExpiryDate(new Date(System.currentTimeMillis() + cookie.getMaxAge() * 1000L));
        }
        if (cookie.hasComment()) {
            storedCookie.setComment(cookie.getComment());
        }
        if (cookie.hasVersion()) {
            storedCookie.setVersion(cookie.getVersion());
        }
        storedCookie.setHttpOnly(cookie.isHttpOnly());
        cookieStore.add(storedCookie);
        return this;
    }

    /**
     * @deprecated HttpClient 4 types are removed in REST Assured 7.0.0, where REST Assured itself knows the URI a
     * response came from after redirects.
     */
    @Deprecated
    public static URI extractEffectiveUriFromContext(HttpContext context) {
        HttpHost targetHost = (HttpHost) context.getAttribute("http.target_host");
        org.apache.http.cookie.CookieOrigin cookieOrigin = (org.apache.http.cookie.CookieOrigin) context.getAttribute("http.cookie-origin");

        if (targetHost == null || cookieOrigin == null) {
            throw new IllegalStateException("Missing target_host or cookie-origin in context");
        }

        String scheme = targetHost.getSchemeName();
        String host = targetHost.getHostName();
        int port = targetHost.getPort();

        // From cookie-origin
        String path = cookieOrigin.getPath();

        try {
            return new URI(scheme, null, host, port, path, null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to build effective URI", e);
        }
    }
}
