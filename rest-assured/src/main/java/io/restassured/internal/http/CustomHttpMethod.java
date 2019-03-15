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

import io.restassured.internal.common.assertion.AssertParameter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

public class CustomHttpMethod extends HttpEntityEnclosingRequestBase {
    private final String methodName;

    public CustomHttpMethod(String methodName, final String uri) {
        this(methodName, URI.create(uri));
    }

    public CustomHttpMethod(String methodName, final URI uri) {
        AssertParameter.notNull(methodName, "Method");
        this.methodName = StringUtils.trim(methodName).toUpperCase();
        setURI(uri);
    }

    public String getMethod() {
        return methodName;
    }
}