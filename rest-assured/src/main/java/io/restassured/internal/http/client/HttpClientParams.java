/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.internal.http.client;

import io.restassured.config.HttpClientConfig;
import io.restassured.internal.support.VersionFiles;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Apache HttpClient 4 parameters of {@link HttpClientConfig#setParam(String, Object)} that REST Assured still
 * reads, which are the ones its own settings are built on: the timeouts, the cookie settings, the user agent and
 * {@code Expect: 100-continue}. The four redirect parameters have never had an effect, since the
 * {@link io.restassured.config.RedirectConfig} outranks them, and say so in a warning. Every other parameter was a
 * setting of HttpClient 4 that REST Assured no longer has a client to set it on, so it fails the request and names
 * what to use instead.
 */
public final class HttpClientParams {
    /**
     * The parameter of the {@code User-Agent} header that a request without one sends.
     */
    public static final String USER_AGENT = "http.useragent";

    private static final Map<String, String> REDIRECT_PARAMETERS = Map.of(
            "http.protocol.handle-redirects", "redirectConfig().followRedirects(..)",
            "http.protocol.max-redirects", "redirectConfig().maxRedirects(..)",
            "http.protocol.allow-circular-redirects", "redirectConfig().allowCircularRedirects(..)",
            "http.protocol.reject-relative-redirect", "redirectConfig().rejectRelativeRedirect(..)");

    private static final Map<String, String> SUPPORTED_PARAMETERS = Map.of(
            "http.connection.timeout", "httpClientConfig().connectTimeout(..)",
            "http.socket.timeout", "httpClientConfig().readTimeout(..)",
            "http.conn-manager.timeout", "httpClientConfig().connectionRequestTimeout(..)",
            "http.protocol.cookie-policy", "httpClientConfig().keepResponseCookies(..)",
            "http.protocol.cookie-datepatterns", "httpClientConfig().cookieDatePatterns(..)",
            "http.protocol.expect-continue", "httpClientConfig().expectContinue(..)",
            USER_AGENT, "a User-Agent header on the request or in the HeaderConfig");

    private static final Map<String, String> REPLACEMENTS = replacements();

    private HttpClientParams() {
    }

    /**
     * Checks that REST Assured can use every parameter of the config, and that their values are of a type it can
     * read. The parameters that only HttpClient 4 could use fail the request, since dropping a setting silently is
     * worse than failing.
     *
     * @param httpClientConfig the HTTP client config of the request
     * @return the {@code User-Agent} that a request without one sends, or {@code null} if there is none
     * @throws IllegalArgumentException if a parameter isn't used any more, or its value isn't a String, a number, a
     *                                  boolean or a list of Strings
     */
    public static String validate(HttpClientConfig httpClientConfig) {
        String userAgent = null;
        for (Map.Entry<String, ?> parameter : httpClientConfig.params().entrySet()) {
            String name = parameter.getKey();
            String replacement = REPLACEMENTS.get(name);
            if (replacement == null) {
                throw new IllegalArgumentException("The Apache HttpClient 4 parameter \"" + name + "\" of the HttpClientConfig "
                        + "isn't used any more, since REST Assured no longer sends its requests with HttpClient 4. Use the settings of "
                        + "the HttpClientConfig, such as connectTimeout(..), readTimeout(..), keepResponseCookies(..) and "
                        + "expectContinue(..), the RedirectConfig for redirects, the request or the HeaderConfig for headers, "
                        + "proxy(..) for a proxy, and httpClientConfig().customizeClient(..) for the settings of the client itself.");
            }
            if (!isSupportedValue(parameter.getValue())) {
                throw new IllegalArgumentException("The value of the Apache HttpClient 4 parameter \"" + name + "\" of the "
                        + "HttpClientConfig must be a String, a number, a boolean or a list of Strings, but was "
                        + parameter.getValue().getClass().getName() + ". Use " + replacement + " instead.");
            }
            if (REDIRECT_PARAMETERS.containsKey(name)) {
                VersionFiles.warnOnce("The Apache HttpClient 4 parameter \"" + name + "\" of the HttpClientConfig has no effect, "
                        + "since the RedirectConfig decides how REST Assured follows redirects. Use " + replacement + ", or "
                        + "given().redirects() for one request.");
            } else if (name.equals(USER_AGENT)) {
                userAgent = parameter.getValue() == null ? null : String.valueOf(parameter.getValue());
            }
        }
        return userAgent;
    }

    private static boolean isSupportedValue(Object value) {
        if (value == null || value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return true;
        }
        if (!(value instanceof Collection)) {
            return false;
        }
        for (Object element : (Collection<?>) value) {
            if (!(element instanceof CharSequence)) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, String> replacements() {
        Map<String, String> replacements = new LinkedHashMap<>(SUPPORTED_PARAMETERS);
        replacements.putAll(REDIRECT_PARAMETERS);
        return Map.copyOf(replacements);
    }
}
