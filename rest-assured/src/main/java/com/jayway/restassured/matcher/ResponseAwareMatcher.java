/*
 * Copyright 2014 the original author or authors.
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

package com.jayway.restassured.matcher;

import com.jayway.restassured.internal.assertion.AssertParameter;
import com.jayway.restassured.response.ResponseBody;
import com.jayway.restassured.response.ResponseOptions;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;

/**
 * An interface that can be implemented to create a {@link org.hamcrest.Matcher} based on the contents of a response. For example imagine that a
 * resource "/x" returns the following JSON document:
 * <pre>
 * {
 *      "userId" : "my-id",
 *      "href" : "http://localhost:8080/my-id"
 * }
 * </pre>
 * you can then verify the href using:
 * <pre>
 * get("/x").then().body("href", new ResponseAwareMatcher<Response>() {
 *                       public Matcher<? extends Object> matcher(Response response) {
 *                           return equalTo("http://localhost:8080/" + response.path("userId"));
 *                       }
 *                  });
 * </pre>
 * ResponseAwareMatchers are also composable with other ResponseAwareMatchers and other Hamcrest matchers.
 * <p>
 * Note that you can also use some of the predefined methods in {@link com.jayway.restassured.matcher.RestAssuredMatchers}.
 * </p>
 *
 * @param <T> The type of the response.
 */
public abstract class ResponseAwareMatcher<T extends ResponseBody<T> & ResponseOptions<T>> {

    /**
     * Create the matcher based on the content of the response.
     *
     * @param response The response.
     * @return The matcher
     * @throws java.lang.Exception Throws any exception if needed
     */
    public abstract Matcher<?> matcher(T response) throws Exception;

    /**
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with another {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     *
     * @param responseAwareMatcher The matcher.
     * @return A composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public final ResponseAwareMatcher<T> and(final ResponseAwareMatcher<T> responseAwareMatcher) {
        AssertParameter.notNull(responseAwareMatcher, ResponseAwareMatcher.class);
        return new ResponseAwareMatcher<T>() {
            public Matcher<?> matcher(T response) throws Exception {
                return allOf((Matcher<Object>) ResponseAwareMatcher.this.matcher(response), (Matcher<Object>) responseAwareMatcher.matcher(response));
            }
        };
    }

    /**
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with a {@link org.hamcrest.Matcher}.
     *
     * @param matcher The matcher.
     * @return A composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public final ResponseAwareMatcher<T> and(final Matcher<? extends Object> matcher) {
        AssertParameter.notNull(matcher, Matcher.class);
        return new ResponseAwareMatcher<T>() {
            public Matcher<?> matcher(T response) throws Exception {
                return allOf((Matcher<Object>) ResponseAwareMatcher.this.matcher(response), (Matcher<Object>) matcher);
            }
        };
    }

    /**
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with another {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     *
     * @param responseAwareMatcher The matcher.
     * @return A composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public final ResponseAwareMatcher<T> or(final ResponseAwareMatcher<T> responseAwareMatcher) {
        AssertParameter.notNull(responseAwareMatcher, ResponseAwareMatcher.class);
        return new ResponseAwareMatcher<T>() {
            public Matcher<?> matcher(T response) throws Exception {
                return anyOf((Matcher<Object>) ResponseAwareMatcher.this.matcher(response), (Matcher<Object>) responseAwareMatcher.matcher(response));
            }
        };
    }

    /**
     * Compose this {@link com.jayway.restassured.matcher.ResponseAwareMatcher} with a {@link org.hamcrest.Matcher}.
     *
     * @param matcher The matcher.
     * @return A composed {@link com.jayway.restassured.matcher.ResponseAwareMatcher}.
     */
    @SuppressWarnings("unchecked")
    public final ResponseAwareMatcher<T> or(final Matcher<? extends Object> matcher) {
        AssertParameter.notNull(matcher, Matcher.class);
        return new ResponseAwareMatcher<T>() {
            public Matcher<?> matcher(T response) throws Exception {
                return anyOf((Matcher<Object>) ResponseAwareMatcher.this.matcher(response), (Matcher<Object>) matcher);
            }
        };
    }
}
