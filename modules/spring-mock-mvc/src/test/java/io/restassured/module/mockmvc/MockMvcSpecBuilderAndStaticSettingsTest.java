/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.module.mockmvc;

import io.restassured.module.mockmvc.http.BasePathController;
import io.restassured.module.mockmvc.http.GreetingController;
import io.restassured.module.mockmvc.http.PostController;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecBuilder;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A specification built with {@link MockMvcRequestSpecBuilder} gets the static settings of {@link RestAssuredMockMvc}
 * when it's used, once.
 */
class MockMvcSpecBuilderAndStaticSettingsTest {
    private final AtomicInteger resultHandlerRuns = new AtomicInteger();
    private final AtomicInteger postProcessorRuns = new AtomicInteger();
    private final ResultHandler countingResultHandler = result -> resultHandlerRuns.incrementAndGet();
    private final RequestPostProcessor countingPostProcessor = request -> {
        postProcessorRuns.incrementAndGet();
        return request;
    };

    @AfterEach
    void reset() {
        RestAssuredMockMvc.reset();
    }

    @Test
    void static_result_handlers_post_processors_headers_and_cookies_apply_once_with_a_built_specification() {
        staticSettingsWithHandlersAndHeaders();
        MockMvcRequestSpecification spec = new MockMvcRequestSpecBuilder().addHeader("X-Spec", "1").build();

        MockHttpServletRequest request = given().spec(spec).get("/greeting").mvcResult().getRequest();

        assertThat(resultHandlerRuns).hasValue(1);
        assertThat(postProcessorRuns).hasValue(1);
        assertThat(Collections.list(request.getHeaders("X-Static"))).containsExactly("1");
        assertThat(Collections.list(request.getHeaders("X-Spec"))).containsExactly("1");
        assertThat(request.getCookies()).hasSize(1);
    }

    @Test
    void static_result_handlers_post_processors_headers_and_cookies_apply_once_with_a_specification_made_by_given() {
        staticSettingsWithHandlersAndHeaders();

        MockHttpServletRequest request = given().spec(RestAssuredMockMvc.with().header("X", "1")).get("/greeting").mvcResult().getRequest();

        assertThat(resultHandlerRuns).hasValue(1);
        assertThat(postProcessorRuns).hasValue(1);
        assertThat(Collections.list(request.getHeaders("X-Static"))).containsExactly("1");
        assertThat(Collections.list(request.getHeaders("X"))).containsExactly("1");
        assertThat(request.getCookies()).hasSize(1);
    }

    @Test
    void a_built_specification_takes_the_mock_mvc_and_base_path_of_the_static_settings_when_it_is_used() {
        RestAssuredMockMvc.standaloneSetup(new PostController());
        RestAssuredMockMvc.basePath = "/other";
        MockMvcRequestSpecification spec = new MockMvcRequestSpecBuilder().build();
        RestAssuredMockMvc.standaloneSetup(new BasePathController());
        RestAssuredMockMvc.basePath = "/my-path";

        MockMvcResponse response = given().spec(spec).get("/greetingPath");

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void a_built_specification_sent_on_its_own_needs_the_mock_mvc_on_the_builder() {
        RestAssuredMockMvc.standaloneSetup(new GreetingController());

        assertThatThrownBy(() -> new MockMvcRequestSpecBuilder().build().get("/greeting"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("new MockMvcRequestSpecBuilder().\n\t\tsetMockMvc(..)");
        assertThat(new MockMvcRequestSpecBuilder().setMockMvc(MockMvcBuilders.standaloneSetup(new GreetingController()).build()).build()
                .get("/greeting").statusCode()).isEqualTo(200);
    }

    private void staticSettingsWithHandlersAndHeaders() {
        RestAssuredMockMvc.standaloneSetup(new GreetingController());
        RestAssuredMockMvc.resultHandlers(countingResultHandler);
        RestAssuredMockMvc.postProcessors(countingPostProcessor);
        RestAssuredMockMvc.requestSpecification = new MockMvcRequestSpecBuilder().addHeader("X-Static", "1").addCookie("static", "1").build();
    }
}
