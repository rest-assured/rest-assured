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

package io.restassured.module.mockmvc.internal;

import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.ValidatableResponseOptionsImpl;
import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.internal.log.LogRepository;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
import io.restassured.response.ExtractableResponse;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

public class ValidatableMockMvcResponseImpl extends ValidatableResponseOptionsImpl<ValidatableMockMvcResponse, MockMvcResponse> implements ValidatableMockMvcResponse {

    private final ResultActions resultActions;
    private final MockMvcResponse mockMvcResponse;

    public ValidatableMockMvcResponseImpl(ResultActions resultActions, String contentType, ResponseParserRegistrar rpr, RestAssuredConfig config,
                                          MockMvcResponse response, ExtractableResponse<MockMvcResponse> extractableResponse, LogRepository logRepository) {
        super(rpr, config, ResponseConverter.toStandardResponse(response), extractableResponse, logRepository);
        this.mockMvcResponse = response;
        AssertParameter.notNull(resultActions, ResultActions.class);
        this.resultActions = resultActions;
    }

    public ValidatableMockMvcResponse expect(ResultMatcher resultMatcher) {
        AssertParameter.notNull(resultMatcher, ResultMatcher.class);
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

    public ValidatableMockMvcResponse apply(ResultHandler resultHandler, ResultHandler... resultHandlers) {
        AssertParameter.notNull(resultHandler, ResultMatcher.class);
        try {
            resultActions.andDo(resultHandler);
        } catch (Exception e) {
            SafeExceptionRethrower.safeRethrow(e);
        }
        for (ResultHandler handler : resultHandlers) {
            try {
                resultActions.andDo(handler);
            } catch (Exception e) {
                SafeExceptionRethrower.safeRethrow(e);
            }
        }
        return this;
    }

    public ValidatableMockMvcResponse status(HttpStatus expectedStatus) {
        statusCode(expectedStatus.value());
        return this;
    }

    public MockMvcResponse originalResponse() {
        return mockMvcResponse;
    }
}
