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
package io.restassured.specification;

import io.restassured.builder.RequestSpecBuilder;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

public class SpecificationQuerierTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test public void
    specification_querier_allows_querying_request_specifications() {
        // Given
        RequestSpecification spec = new RequestSpecBuilder().addHeader("header", "value").addCookie("cookie", "cookieValue").addParam("someparam", "somevalue").build();

        // When
        QueryableRequestSpecification queryable = SpecificationQuerier.query(spec);

        // Then
        softly.assertThat(queryable.getHeaders().getValue("header")).isEqualTo("value");
        softly.assertThat(queryable.getCookies().getValue("cookie")).isEqualTo("cookieValue");
        softly.assertThat(queryable.getRequestParams().get("someparam")).isEqualTo("somevalue");
    }
}