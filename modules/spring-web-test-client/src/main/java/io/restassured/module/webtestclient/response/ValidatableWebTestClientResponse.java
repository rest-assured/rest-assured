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

package io.restassured.module.webtestclient.response;

import io.restassured.response.ValidatableResponseOptions;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * A validatable response of a request made by REST Assured WebTestClient. Returns an instance of
 * {@link WebTestClient.ResponseSpec} so that user can make subsequent assertions using the
 * {@link WebTestClient.ResponseSpec} API.
 */
public interface ValidatableWebTestClientResponse extends ValidatableResponseOptions<ValidatableWebTestClientResponse, WebTestClientResponse> {
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
    ValidatableWebTestClientResponse status(HttpStatus expectedStatus);
}
