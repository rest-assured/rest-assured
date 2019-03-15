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

import io.restassured.config.LogConfig;
import io.restassured.internal.RestAssuredResponseOptionsImpl;
import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.internal.log.LogRepository;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

public class MockMvcRestAssuredResponseImpl extends RestAssuredResponseOptionsImpl<MockMvcResponse> implements MockMvcResponse {

    private final ResultActions resultActions;
    private final LogRepository logRepository;

    public MockMvcRestAssuredResponseImpl(ResultActions resultActions, LogRepository logRepository) {
        AssertParameter.notNull(resultActions, ResultActions.class);
        AssertParameter.notNull(logRepository, LogRepository.class);
        this.resultActions = resultActions;
        this.logRepository = logRepository;
    }

    public ValidatableMockMvcResponse then() {
        ValidatableMockMvcResponseImpl response = new ValidatableMockMvcResponseImpl(resultActions, getContentType(), getRpr(), getConfig(), this, this, logRepository);
        LogConfig logConfig = getConfig().getLogConfig();
        if (logConfig.isLoggingOfRequestAndResponseIfValidationFailsEnabled()) {
            response.log().ifValidationFails(logConfig.logDetailOfRequestAndResponseIfValidationFails(), logConfig.isPrettyPrintingEnabled());
        }
        return response;
    }

    public MvcResult mvcResult() {
        return resultActions.andReturn();
    }

    public MvcResult getMvcResult() {
        return mvcResult();
    }

    public MockHttpServletResponse mockHttpServletResponse() {
        return mvcResult().getResponse();
    }

    public MockHttpServletResponse getMockHttpServletResponse() {
        return mockHttpServletResponse();
    }
}
