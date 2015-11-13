/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.response;

import org.hamcrest.Matcher;

import java.util.concurrent.TimeUnit;

/**
 * A validatable response of a request made by REST Assured.
 * <p>
 * Usage example:
 * <pre>
 * get("/lotto").then().body("lotto.lottoId", is(5));
 * </pre>
 * </p>
 */
public interface ValidatableResponse extends ValidatableResponseOptions<ValidatableResponse, Response> {

    /**
     * Validate that the response time (in milliseconds) matches the supplied <code>matcher</code>. For example:
     * <p/>
     * <pre>
     * when().
     *        get("/something").
     * then().
     *        responseTime(lessThan(2000));
     * </pre>
     * <p/>
     * where <code>lessThan</code> is a Hamcrest matcher
     *
     * @return The {@link ValidatableResponse} instance.
     */
    ValidatableResponse responseTime(Matcher<Long> matcher);

    /**
     * Validate that the response time matches the supplied <code>matcher</code> and time unit. For example:
     * <p/>
     * <pre>
     * when().
     *        get("/something").
     * then().
     *        responseTime(lessThan(2), TimeUnit.SECONDS);
     * </pre>
     * <p/>
     * where <code>lessThan</code> is a Hamcrest matcher
     *
     * @return The {@link ValidatableResponse} instance.
     */
    ValidatableResponse responseTime(Matcher<Long> matcher, TimeUnit timeUnit);
}
