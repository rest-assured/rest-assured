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

package io.restassured.module.mockmvc.response;

import io.restassured.response.ValidatableResponseOptions;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * A validatable response of a request made by REST Assured Mock Mvc.
 * <p>
 * Usage example:
 * <pre>
 * get("/lotto").then().body("lotto.lottoId", is(5));
 * </pre>
 * </p>
 */
public interface ValidatableMockMvcResponse extends ValidatableResponseOptions<ValidatableMockMvcResponse, MockMvcResponse> {
    /**
     * Expect a {@link org.springframework.test.web.servlet.ResultMatcher} to match the response.
     * <p/>
     * <p>Note same as {@link #assertThat(org.springframework.test.web.servlet.ResultMatcher)} but a different syntax.</p>
     *
     * @param resultMatcher The result matcher
     * @return The {@link ValidatableMockMvcResponse} instance.
     */
    ValidatableMockMvcResponse expect(ResultMatcher resultMatcher);

    /**
     * Assert that a {@link org.springframework.test.web.servlet.ResultMatcher} matches the response.
     * <p/>
     * <p>Note same as {@link #expect(org.springframework.test.web.servlet.ResultMatcher)} but a different syntax.</p>
     *
     * @param resultMatcher The result matcher
     * @return The {@link ValidatableMockMvcResponse} instance.
     */
    ValidatableMockMvcResponse assertThat(ResultMatcher resultMatcher);

    /**
     * Apply one or more result handlers.
     *
     * @param resultHandler The first result handler
     * @param resultHandlers Additional result handlers to add (optional)
     * @return The {@link ValidatableMockMvcResponse} instance.
     */
    ValidatableMockMvcResponse apply(ResultHandler resultHandler, ResultHandler... resultHandlers);

    /**
     * Validate that the response status matches an Spring-Framework HttpStatus. E.g.
     * <pre>
     * get("/something").then().assertThat().status(HttpStatus.OK);
     * </pre>
     * <p/>
     *
     * @param expectedStatus The expected status.
     * @return the response specification
     */
    ValidatableMockMvcResponse status(HttpStatus expectedStatus);
}
