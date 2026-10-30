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

package io.restassured.module.webtestclient.internal;

import io.restassured.internal.client.ResponseDefaults;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.function.Supplier;

/**
 * The defaults that each <code>given()</code> of a WebTestClient client starts from, the values of the static fields of
 * {@link io.restassured.module.webtestclient.RestAssuredWebTestClient}. <code>given()</code> only reads them, so it can
 * be called from many threads at once.
 *
 * @param webTestClientFactory The factory of the WebTestClient instance, or <code>null</code> for none
 * @param config               The config, or <code>null</code> for the default one
 */
public record WebTestClientClientDefaults(WebTestClientFactory webTestClientFactory, RestAssuredWebTestClientConfig config, String basePath,
                                          WebTestClientRequestSpecification requestSpecification,
                                          ResponseSpecification responseSpecification) {

    /**
     * @param responseDefaults Gives the defaults of <code>then()</code> of the client
     * @return A new request specification with these defaults
     */
    public WebTestClientRequestSpecification given(Supplier<ResponseDefaults> responseDefaults) {
        return new WebTestClientRequestSpecificationImpl(webTestClientFactory, config, basePath, requestSpecification, responseSpecification,
                responseDefaults);
    }
}
