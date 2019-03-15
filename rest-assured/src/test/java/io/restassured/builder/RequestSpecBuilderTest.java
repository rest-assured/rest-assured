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

package io.restassured.builder;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.internal.RequestSpecificationImpl;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class RequestSpecBuilderTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test public void
    request_spec_doesnt_throw_NPE_when_logging_after_creation() {
        new RequestSpecBuilder().build().log().all(true);
    }

    @Test public void
    request_spec_picks_up_filters_from_static_config() {
        RestAssured.filters(new RequestLoggingFilter());
        try {
            RequestSpecBuilder builder = new RequestSpecBuilder();
            RequestSpecificationImpl spec = (RequestSpecificationImpl) builder.build();
            Assert.assertThat(spec.getDefinedFilters(), hasSize(1));
        } finally {
            RestAssured.reset();
        }
    }

    @Test public void
    request_spec_picks_up_headers_from_static_request_spec() {
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .addHeader("hello", "world")
                .build();
        try {
            RequestSpecBuilder builder = new RequestSpecBuilder();
            RequestSpecificationImpl spec = (RequestSpecificationImpl) builder.build();
            Assert.assertThat(spec.getHeaders().getValue("hello"), equalTo("world"));
        } finally {
            RestAssured.reset();
        }
    }
}
