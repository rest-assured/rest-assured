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

import com.jayway.restassured.internal.RestAssuredResponseOptionsImpl;
import com.jayway.restassured.internal.log.LogRepository;
import com.jayway.restassured.module.mockmvc.response.MockMvcResponse;
import com.jayway.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;

public class MockMvcRestAssuredResponseImpl extends RestAssuredResponseOptionsImpl<MockMvcResponse> implements MockMvcResponse {

    private final ResultActions resultActions;
    private final LogRepository logRepository;

    public MockMvcRestAssuredResponseImpl(ResultActions resultActions, LogRepository logRepository) {
        notNull(resultActions, ResultActions.class);
        notNull(logRepository, LogRepository.class);
        this.resultActions = resultActions;
        this.logRepository = logRepository;
    }

    public ValidatableMockMvcResponse then() {
        return new ValidatableMockMvcResponseImpl(resultActions, getContentType(), getRpr(), getConfig(), this, this, logRepository);
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
