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
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.http.Header;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.DefaultCookieSpec;
import org.apache.http.message.BasicHeader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * The cookie filter can be used to keep track of all the cookies sent by the server and use them in subsequent requests.
 * It matches the cookies to the request URL within the rules implemented in {@link DefaultCookieSpec}.
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
    private final BasicCookieStore cookieStore;

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
        this.cookieSpec = new DefaultCookieSpec();
        this.cookieStore = new BasicCookieStore();
    }

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {

        final CookieOrigin cookieOrigin = cookieOriginFromUri(requestSpec.getURI());
        for (Cookie cookie : cookieStore.getCookies()) {
            if (cookieSpec.match(cookie, cookieOrigin) && allowMultipleCookiesWithTheSameNameOrCookieNotPreviouslyDefined(requestSpec, cookie)) {
                requestSpec.cookie(cookie.getName(), cookie.getValue());
            }
        }

        final Response response = ctx.next(requestSpec, responseSpec);

        List<Cookie> responseCookies = extractResponseCookies(response, cookieOrigin);
        cookieStore.addCookies(responseCookies.toArray(new Cookie[0]));
        return response;
    }

    private boolean allowMultipleCookiesWithTheSameNameOrCookieNotPreviouslyDefined(FilterableRequestSpecification requestSpec, Cookie cookie) {
        return allowMultipleCookiesWithTheSameName || !requestSpec.getCookies().hasCookieWithName(cookie.getName());
    }

    private List<Cookie> extractResponseCookies(Response response, CookieOrigin cookieOrigin) {

        List<Cookie> cookies = new ArrayList<Cookie>();
        for (String cookieValue : response.getHeaders().getValues("Set-Cookie")) {
            Header setCookieHeader = new BasicHeader("Set-Cookie", cookieValue);
            try {
                cookies.addAll(cookieSpec.parse(setCookieHeader, cookieOrigin));
            } catch (MalformedCookieException ignored) {
            }
        }
        return cookies;
    }

    private CookieOrigin cookieOriginFromUri(String uri) {

        try {
            URL parsedUrl = new URL(uri);
            int port = parsedUrl.getPort() != -1 ? parsedUrl.getPort() : 80;
            return new CookieOrigin(
                    parsedUrl.getHost(), port, parsedUrl.getPath(), "https".equals(parsedUrl.getProtocol()));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
