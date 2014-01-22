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

package com.jayway.restassured.module.mockmvc.internal;

import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.internal.ResponseParserRegistrar;
import com.jayway.restassured.internal.ValidatableResponseOptionsImpl;
import com.jayway.restassured.internal.util.SafeExceptionRethrower;
import com.jayway.restassured.module.mockmvc.response.MockMvcResponse;
import com.jayway.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
import com.jayway.restassured.response.ExtractableResponse;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;
import static com.jayway.restassured.module.mockmvc.internal.ResponseConverter.toStandardResponse;

public class ValidatableMockMvcResponseImpl extends ValidatableResponseOptionsImpl<ValidatableMockMvcResponse, MockMvcResponse> implements ValidatableMockMvcResponse {

    private final ResultActions resultActions;
    private final MockMvcResponse mockMvcResponse;

    public ValidatableMockMvcResponseImpl(ResultActions resultActions, String contentType, ResponseParserRegistrar rpr, RestAssuredConfig config,
                                          MockMvcResponse response, ExtractableResponse<MockMvcResponse> extractableResponse) {
        super(contentType, rpr, config, toStandardResponse(response), extractableResponse);
        this.mockMvcResponse = response;
        notNull(resultActions, ResultActions.class);
        this.resultActions = resultActions;
    }

    public ValidatableMockMvcResponse expect(ResultMatcher resultMatcher) {
        notNull(resultMatcher, ResultMatcher.class);
        try {
            resultActions.andExpect(resultMatcher);
        } catch (Exception e) {
            SafeExceptionRethrower.safeRethrow(e);
        }
        return this;
    }

    public ValidatableMockMvcResponse assertThat(ResultMatcher resultMatcher) {
        return expect(resultMatcher);
    }

    public MockMvcResponse originalResponse() {
        return mockMvcResponse;
    }
}
