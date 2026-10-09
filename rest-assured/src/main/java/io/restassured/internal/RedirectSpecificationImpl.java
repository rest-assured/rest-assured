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

import io.restassured.specification.RedirectSpecification;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static org.apache.http.client.params.ClientPNames.ALLOW_CIRCULAR_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.HANDLE_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.MAX_REDIRECTS;
import static org.apache.http.client.params.ClientPNames.REJECT_RELATIVE_REDIRECT;

public class RedirectSpecificationImpl implements RedirectSpecification {
    private final RequestSpecification requestSpecification;
    private final Map<String, Object> httpClientParams;

    public RedirectSpecificationImpl(RequestSpecification requestSpecification, Map<String, Object> httpClientParams) {
        this.requestSpecification = requestSpecification;
        this.httpClientParams = httpClientParams;
    }

    @Override
    public RequestSpecification max(int maxNumberOfRedirect) {
        httpClientParams.put(MAX_REDIRECTS, maxNumberOfRedirect);
        return requestSpecification;
    }

    @Override
    public RequestSpecification follow(boolean followRedirects) {
        httpClientParams.put(HANDLE_REDIRECTS, followRedirects);
        return requestSpecification;
    }

    @Override
    public RequestSpecification allowCircular(boolean allowCircularRedirects) {
        httpClientParams.put(ALLOW_CIRCULAR_REDIRECTS, allowCircularRedirects);
        return requestSpecification;
    }

    @Override
    public RequestSpecification rejectRelative(boolean rejectRelativeRedirects) {
        httpClientParams.put(REJECT_RELATIVE_REDIRECT, rejectRelativeRedirects);
        return requestSpecification;
    }
}
