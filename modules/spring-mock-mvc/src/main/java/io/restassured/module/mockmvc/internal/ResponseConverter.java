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

import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.response.Response;

class ResponseConverter {
    static Response toStandardResponse(MockMvcResponse response) {
        if (!(response instanceof MockMvcRestAssuredResponseImpl)) {
            throw new IllegalArgumentException(MockMvcResponse.class.getName() + " must be an instance of " + MockMvcRestAssuredResponseImpl.class.getName());
        }
        MockMvcRestAssuredResponseImpl mvc = (MockMvcRestAssuredResponseImpl) response;

        RestAssuredResponseImpl std = new RestAssuredResponseImpl();
        std.setConnectionManager(mvc.getConnectionManager());
        std.setContent(mvc.getContent());
        std.setContentType(mvc.getContentType());
        std.setCookies(mvc.detailedCookies());
        std.setDecoderConfig(mvc.getDecoderConfig());
        std.setDefaultContentType(mvc.getDefaultContentType());
        std.setHasExpectations(mvc.getHasExpectations());
        std.setResponseHeaders(mvc.getResponseHeaders());
        std.setSessionIdName(mvc.getSessionIdName());
        std.setStatusCode(mvc.getStatusCode());
        std.setStatusLine(mvc.getStatusLine());
        std.setRpr(mvc.getRpr());
        std.setFilterContextProperties(mvc.getFilterContextProperties());
        std.setLogRepository(mvc.getLogRepository());
        return std;
    }
}
