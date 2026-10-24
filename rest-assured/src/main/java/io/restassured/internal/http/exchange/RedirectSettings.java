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

package io.restassured.internal.http.exchange;

import io.restassured.config.RedirectConfig;

/**
 * How REST Assured follows the redirects of one request.
 *
 * @param follow                whether a redirect is followed at all
 * @param max                   how many redirects are followed before the request fails
 * @param allowCircular         whether a redirect to a URI the request has already been to is followed
 * @param rejectRelative        whether a {@code Location} that isn't an absolute URI fails the request
 * @param followAllMethods      whether the redirects of every method are followed, not only of GET and HEAD
 * @param stripSensitiveHeaders whether {@code Authorization} and {@code Cookie} are dropped from a redirect to
 *                              another scheme, host or port
 */
public record RedirectSettings(boolean follow, int max, boolean allowCircular, boolean rejectRelative,
                               boolean followAllMethods, boolean stripSensitiveHeaders) {

    /**
     * What a request sets with {@code given().redirects()}, which outranks the {@link RedirectConfig}.
     */
    public static final class Overrides {
        private Boolean follow;
        private Integer max;
        private Boolean allowCircular;
        private Boolean rejectRelative;

        /**
         * @return a copy, so that sending a request doesn't change the specification it was sent from
         */
        public Overrides copy() {
            Overrides copy = new Overrides();
            copy.follow = follow;
            copy.max = max;
            copy.allowCircular = allowCircular;
            copy.rejectRelative = rejectRelative;
            return copy;
        }

        public void follow(boolean follow) {
            this.follow = follow;
        }

        public void max(int max) {
            this.max = max;
        }

        public void allowCircular(boolean allowCircular) {
            this.allowCircular = allowCircular;
        }

        public void rejectRelative(boolean rejectRelative) {
            this.rejectRelative = rejectRelative;
        }

        /**
         * @param config the redirect config of the request, or {@code null} for the defaults
         * @return the settings of the request: what it set itself, and the config for the rest
         */
        public RedirectSettings resolve(RedirectConfig config) {
            RedirectConfig redirects = config == null ? new RedirectConfig() : config;
            return new RedirectSettings(
                    follow == null ? redirects.followsRedirects() : follow,
                    max == null ? redirects.maxRedirects() : max,
                    allowCircular == null ? redirects.allowsCircularRedirects() : allowCircular,
                    rejectRelative == null ? redirects.rejectRelativeRedirects() : rejectRelative,
                    redirects.followsRedirectsForAllMethods(),
                    redirects.stripsSensitiveHeadersOnCrossHostRedirect());
        }
    }
}
