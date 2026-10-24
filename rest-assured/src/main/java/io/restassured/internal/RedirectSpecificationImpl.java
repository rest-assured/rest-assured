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

package io.restassured.internal;

import io.restassured.internal.http.exchange.RedirectSettings;
import io.restassured.specification.RedirectSpecification;
import io.restassured.specification.RequestSpecification;

public class RedirectSpecificationImpl implements RedirectSpecification {
    private final RequestSpecification requestSpecification;
    private final RedirectSettings.Overrides redirects;

    public RedirectSpecificationImpl(RequestSpecification requestSpecification, RedirectSettings.Overrides redirects) {
        this.requestSpecification = requestSpecification;
        this.redirects = redirects;
    }

    @Override
    public RequestSpecification max(int maxNumberOfRedirect) {
        redirects.max(maxNumberOfRedirect);
        return requestSpecification;
    }

    @Override
    public RequestSpecification follow(boolean followRedirects) {
        redirects.follow(followRedirects);
        return requestSpecification;
    }

    @Override
    public RequestSpecification allowCircular(boolean allowCircularRedirects) {
        redirects.allowCircular(allowCircularRedirects);
        return requestSpecification;
    }

    @Override
    public RequestSpecification rejectRelative(boolean rejectRelativeRedirects) {
        redirects.rejectRelative(rejectRelativeRedirects);
        return requestSpecification;
    }
}
