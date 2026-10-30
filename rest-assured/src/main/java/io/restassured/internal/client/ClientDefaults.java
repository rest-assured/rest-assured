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

package io.restassured.internal.client;

import io.restassured.authentication.AuthenticationScheme;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.internal.TestSpecificationImpl;
import io.restassured.internal.log.LogRepository;
import io.restassured.parsing.Parser;
import io.restassured.specification.ProxySpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.List;
import java.util.function.Supplier;

/**
 * The defaults that each <code>given()</code> of a client starts from, the values of the static fields of
 * {@link io.restassured.RestAssured}. <code>given()</code> only reads them, including the specifications, the filter list
 * and the parser registry, so it can be called from many threads at once.
 *
 * @param config    The config without the session id
 * @param sessionId The session id, which {@link #configWithSessionId()} applies to the config, or <code>null</code>
 */
public record ClientDefaults(String baseUri, int port, String basePath, boolean urlEncodingEnabled, AuthenticationScheme authentication,
                             RestAssuredConfig config, String rootPath, RequestSpecification requestSpecification,
                             ResponseSpecification responseSpecification, Parser defaultParser, String sessionId, ProxySpecification proxy,
                             List<Filter> filters, ResponseParserRegistrar parsers) {

    /**
     * @param newRequest       Starts a request with the defaults of the client, for the extra requests of form
     *                         authentication and CSRF
     * @param responseDefaults Gives the defaults of <code>then()</code> of the client
     * @return A new request and response specification, linked to each other, with these defaults
     */
    public TestSpecificationImpl testSpecification(Supplier<RequestSpecification> newRequest, Supplier<ResponseDefaults> responseDefaults) {
        ResponseParserRegistrar responseParserRegistrar = new ResponseParserRegistrar(parsers);
        if (defaultParser != null) {
            responseParserRegistrar.registerDefaultParser(defaultParser);
        }
        LogRepository logRepository = new LogRepository();
        RestAssuredConfig restAssuredConfig = configWithSessionId();
        return TestSpecificationImpl.linked(
                new RequestSpecificationImpl(baseUri, port, basePath, authentication, filters, requestSpecification, urlEncodingEnabled,
                        restAssuredConfig, logRepository, proxy, true, true, newRequest, responseDefaults),
                new ResponseSpecificationImpl(rootPath, responseSpecification, responseParserRegistrar, restAssuredConfig, logRepository));
    }

    /**
     * @return The config with the session id as session id value, if there is one
     */
    public RestAssuredConfig configWithSessionId() {
        return sessionId == null ? config : config.sessionConfig(config.getSessionConfig().sessionIdValue(sessionId));
    }
}
