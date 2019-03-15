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

package io.restassured.matcher;

import io.restassured.response.ResponseBody;
import io.restassured.response.ResponseOptions;
import org.hamcrest.Matcher;

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
 * Note that you can also use some of the predefined methods in {@link RestAssuredMatchers}.
 * </p>
 *
 * @param <T> The type of the response.
 */
@FunctionalInterface
public interface ResponseAwareMatcher<T extends ResponseBody<T> & ResponseOptions<T>> {

    /**
     * Create the matcher based on the content of the response.
     *
     * @param response The response.
     * @return The matcher
     * @throws java.lang.Exception Throws any exception if needed
     */
    Matcher<?> matcher(T response) throws Exception;
}