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
package io.restassured.internal;

import io.restassured.http.Header;
import io.restassured.internal.http.HttpResponseDecorator;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;

import java.util.List;

public class RestAssuredResponseImpl extends RestAssuredResponseOptionsImpl<Response> implements Response {

    public void parseResponse(HttpResponseDecorator httpResponse, Object content, boolean hasBodyAssertions, ResponseParserRegistrar responseParserRegistrar) {
        groovyResponse.parseResponse(httpResponse, content, hasBodyAssertions, responseParserRegistrar);
    }

    @Override
    public ValidatableResponse then() {
        return new ValidatableResponseImpl(getContentType(), getRpr(), getConfig(), this, this, getLogRepository());
    }

    public String prettyPrintHeaders() {
        List<Header> headers = headers().asList();
        StringBuilder headersStringBuilder = new StringBuilder("Headers:\t\t");

        for (int i = 0; i < headers.size(); i++) {
            if (i != 0) {
                headersStringBuilder.append("\t\t\t\t");
            }

            Header currentHeader = headers.get(i);
            headersStringBuilder.append(String.format("%s=%s\n", currentHeader.getName(), currentHeader.getValue()));
        }

        System.out.println(headersStringBuilder);

        return headersStringBuilder.toString();
    }
}