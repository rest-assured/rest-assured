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

import com.jayway.restassured.authentication.NoAuthScheme;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.log.LogDetail;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.internal.LogSpecificationImpl;
import com.jayway.restassured.internal.RequestSpecificationImpl;
import com.jayway.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import com.jayway.restassured.module.mockmvc.specification.MockMvcRequestLogSpecification;

import java.util.Collections;

public class MockMvcRequestLogSpecificationImpl extends LogSpecificationImpl implements MockMvcRequestLogSpecification {
    private MockMvcRequestSpecificationImpl requestSpecification;

    public MockMvcRequestLogSpecificationImpl(MockMvcRequestSpecificationImpl requestSpecification) {
        this.requestSpecification = requestSpecification;
    }

    public MockMvcRequestSpecification params() {
        return logWith(LogDetail.PARAMS);
    }

    public MockMvcRequestSpecification parameters() {
        return logWith(LogDetail.PARAMS);
    }

    public MockMvcRequestSpecification body() {
        return body(shouldPrettyPrint(toRequestSpecification()));
    }

    public MockMvcRequestSpecification body(boolean shouldPrettyPrint) {
        return logWith(LogDetail.BODY, shouldPrettyPrint);
    }

    public MockMvcRequestSpecification all(boolean shouldPrettyPrint) {
        return logWith(LogDetail.ALL, shouldPrettyPrint);
    }

    public MockMvcRequestSpecification everything(boolean shouldPrettyPrint) {
        return all(shouldPrettyPrint);
    }

    public MockMvcRequestSpecification all() {
        return all(shouldPrettyPrint(toRequestSpecification()));
    }

    public MockMvcRequestSpecification everything() {
        return all();
    }

    public MockMvcRequestSpecification headers() {
        return logWith(LogDetail.HEADERS);
    }

    public MockMvcRequestSpecification cookies() {
        return logWith(LogDetail.COOKIES);
    }

    private MockMvcRequestSpecification logWith(LogDetail logDetail) {
        RequestSpecificationImpl reqSpec = toRequestSpecification();
        return logWith(logDetail, shouldPrettyPrint(reqSpec));
    }

    private MockMvcRequestSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled) {
        requestSpecification.setRequestLoggingFilter(new RequestLoggingFilter(logDetail, prettyPrintingEnabled, getPrintStream(toRequestSpecification())));
        return requestSpecification;
    }

    private RequestSpecificationImpl toRequestSpecification() {
        return new RequestSpecificationImpl("", 8080, "", new NoAuthScheme(), Collections.<Filter>emptyList(), null, null, true, requestSpecification.getConfig());
    }
}
