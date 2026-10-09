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

package io.restassured.assertion;

import io.restassured.http.Headers;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.matcher.ResponseAwareMatcher;
import io.restassured.response.Response;
import org.hamcrest.Matcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static io.restassured.internal.util.GroovyStringConversion.castToString;

public class HeaderMatcher {

    private Object headerName;
    private Object mappingFunction;
    private Matcher<?> matcher;
    private ResponseAwareMatcher<Response> responseAwareMatcher;

    @SuppressWarnings("unchecked")
    public Map<String, Object> validateHeader(Response response) {
        Headers headers = response.getHeaders();
        boolean success = true;
        String message = "";
        String name = castToString(headerName);
        Object value = headers.getValue(name);
        Matcher<?> effectiveMatcher = matcher;
        if (responseAwareMatcher != null) {
            try {
                effectiveMatcher = responseAwareMatcher.matcher(response);
            } catch (Exception e) {
                return SafeExceptionRethrower.safeRethrow(e);
            }
            if (effectiveMatcher == null) {
                throw new IllegalArgumentException("The ResponseAwareMatcher for header \"" + name + "\" returned null instead of a Hamcrest matcher");
            }
        }
        if (mappingFunction != null) {
            value = ((Function<Object, Object>) mappingFunction).apply(value);
        }
        if (!effectiveMatcher.matches(value)) {
            success = false;
            message = "Expected header \"" + name + "\" was not " + effectiveMatcher + ", was \"" + castToString(value) + "\". Headers are:\n" + headers + "\n";
        }
        Map<String, Object> result = new LinkedHashMap<>(2);
        result.put("success", success);
        result.put("errorMessage", message);
        return result;
    }

    public Object getHeaderName() {
        return headerName;
    }

    public void setHeaderName(Object headerName) {
        this.headerName = headerName;
    }

    public Object getMappingFunction() {
        return mappingFunction;
    }

    public void setMappingFunction(Object mappingFunction) {
        this.mappingFunction = mappingFunction;
    }

    public Matcher<?> getMatcher() {
        return matcher;
    }

    public void setMatcher(Matcher<?> matcher) {
        this.matcher = matcher;
    }

    public ResponseAwareMatcher<Response> getResponseAwareMatcher() {
        return responseAwareMatcher;
    }

    public void setResponseAwareMatcher(ResponseAwareMatcher<Response> responseAwareMatcher) {
        this.responseAwareMatcher = responseAwareMatcher;
    }
}
