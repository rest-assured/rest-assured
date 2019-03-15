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
package io.restassured.module.webtestclient.internal;

import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.response.Response;

class ResponseConverter {

	private ResponseConverter() {
	}

	static Response toStandardResponse(WebTestClientResponse response) {
		if (!(response instanceof WebTestClientRestAssuredResponseImpl)) {
            throw new IllegalArgumentException(WebTestClientResponse.class.getName() + " must be an instance of "
                    + WebTestClientRestAssuredResponseImpl.class.getName());
		}
		WebTestClientRestAssuredResponseImpl webTestClientRestAssuredResponse = (WebTestClientRestAssuredResponseImpl) response;

        RestAssuredResponseImpl standardResponse = new RestAssuredResponseImpl();
        standardResponse.setConnectionManager(webTestClientRestAssuredResponse.getConnectionManager());
        standardResponse.setContent(webTestClientRestAssuredResponse.getContent());
        standardResponse.setContentType(webTestClientRestAssuredResponse.getContentType());
        standardResponse.setCookies(webTestClientRestAssuredResponse.detailedCookies());
        standardResponse.setDecoderConfig(webTestClientRestAssuredResponse.getDecoderConfig());
        standardResponse.setDefaultContentType(webTestClientRestAssuredResponse.getDefaultContentType());
        standardResponse.setHasExpectations(webTestClientRestAssuredResponse.getHasExpectations());
        standardResponse.setResponseHeaders(webTestClientRestAssuredResponse.getResponseHeaders());
        standardResponse.setSessionIdName(webTestClientRestAssuredResponse.getSessionIdName());
        standardResponse.setStatusCode(webTestClientRestAssuredResponse.getStatusCode());
        standardResponse.setStatusLine(webTestClientRestAssuredResponse.getStatusLine());
        standardResponse.setRpr(webTestClientRestAssuredResponse.getRpr());
        standardResponse.setFilterContextProperties(webTestClientRestAssuredResponse.getFilterContextProperties());
        standardResponse.setLogRepository(webTestClientRestAssuredResponse.getLogRepository());
        return standardResponse;
	}

}
