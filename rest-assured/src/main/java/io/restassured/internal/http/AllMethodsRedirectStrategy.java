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

package io.restassured.internal.http;

import org.apache.http.client.RedirectStrategy;
import org.apache.http.impl.client.DefaultRedirectStrategy;

/**
 * Follows redirects for every HTTP method, used for {@link io.restassured.config.RedirectConfig#followRedirectsForAllMethods(boolean)}.
 * It remembers the strategy it replaced, so that a reused client gets that strategy back when a later request doesn't
 * follow redirects for all methods.
 */
public class AllMethodsRedirectStrategy extends DefaultRedirectStrategy {
    private final RedirectStrategy replacedStrategy;

    public AllMethodsRedirectStrategy(RedirectStrategy replacedStrategy) {
        this.replacedStrategy = replacedStrategy;
    }

    public RedirectStrategy getReplacedStrategy() {
        return replacedStrategy;
    }

    @Override
    protected boolean isRedirectable(String method) {
        return true;
    }
}
