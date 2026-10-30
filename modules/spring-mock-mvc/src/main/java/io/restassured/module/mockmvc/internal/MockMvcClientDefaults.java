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

package io.restassured.module.mockmvc.internal;

import io.restassured.internal.client.ResponseDefaults;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.specification.MockMvcAuthenticationScheme;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.function.Supplier;

/**
 * The defaults that each <code>given()</code> of a MockMvc client starts from, the values of the static fields of
 * {@link io.restassured.module.mockmvc.RestAssuredMockMvc}. <code>given()</code> only reads them, so it can be called
 * from many threads at once.
 *
 * @param mockMvcFactory The factory of the MockMvc instance, or <code>null</code> for none
 * @param config         The config, or <code>null</code> for the default one
 */
public record MockMvcClientDefaults(MockMvcFactory mockMvcFactory, RestAssuredMockMvcConfig config, List<ResultHandler> resultHandlers,
                                    List<RequestPostProcessor> requestPostProcessors, String basePath,
                                    MockMvcRequestSpecification requestSpecification, ResponseSpecification responseSpecification,
                                    MockMvcAuthenticationScheme authentication) {

    /**
     * @param responseDefaults Gives the defaults of <code>then()</code> of the client
     * @return A new request specification with these defaults
     */
    public MockMvcRequestSpecification given(Supplier<ResponseDefaults> responseDefaults) {
        return new MockMvcRequestSpecificationImpl(mockMvcFactory, config, resultHandlers, requestPostProcessors, basePath, requestSpecification,
                responseSpecification, authentication, responseDefaults);
    }
}
