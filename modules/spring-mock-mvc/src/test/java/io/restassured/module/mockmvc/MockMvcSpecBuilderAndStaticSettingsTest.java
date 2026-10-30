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

import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.module.mockmvc.http.BasePathController;
import io.restassured.module.mockmvc.http.GreetingController;
import io.restassured.module.mockmvc.http.MultipartController;
import io.restassured.module.mockmvc.http.PostController;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecBuilder;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig.config;
import static java.nio.charset.StandardCharsets.UTF_8;
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
    private final ByteArrayOutputStream log = new ByteArrayOutputStream();
    private final LogConfig blacklistingSecret = logConfig().defaultStream(new PrintStream(log, true)).blacklistHeader("Secret");

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
    void a_static_multipart_part_is_sent_once_with_a_specification_made_by_given() {
        RestAssuredMockMvc.standaloneSetup(new MultipartController());
        RestAssuredMockMvc.requestSpecification = new MockMvcRequestSpecBuilder().addMultiPart("file", "file.txt", "content".getBytes(UTF_8)).build();

        MockMvcResponse response = given().spec(RestAssuredMockMvc.with().header("X", "1")).post("/files/{type}", "text");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(((MockMultipartHttpServletRequest) response.mvcResult().getRequest()).getFiles("file")).hasSize(1);
    }

    @Test
    void a_built_specification_logs_with_the_static_log_config() {
        RestAssuredMockMvc.standaloneSetup(new GreetingController());
        RestAssuredMockMvc.config = config().logConfig(blacklistingSecret);
        MockMvcRequestSpecification specification = new MockMvcRequestSpecBuilder().log(LogDetail.HEADERS).build();

        given().spec(specification).header("Secret", "password").get("/greeting");

        assertThat(log.toString()).contains("Secret=[ BLACKLISTED ]").doesNotContain("password");
    }

    @Test
    void a_built_specification_logs_with_the_log_config_of_the_client() {
        RestAssuredMockMvcClient client = RestAssuredMockMvcClient.builder().standaloneSetup(new GreetingController())
                .config(config().logConfig(blacklistingSecret)).build();
        MockMvcRequestSpecification specification = new MockMvcRequestSpecBuilder().log(LogDetail.HEADERS).build();

        given(client).spec(specification).header("Secret", "password").get("/greeting");

        assertThat(log.toString()).contains("Secret=[ BLACKLISTED ]").doesNotContain("password");
    }

    @Test
    void log_takes_a_log_config_set_after_it() {
        RestAssuredMockMvc.standaloneSetup(new GreetingController());

        given().log().headers().config(config().logConfig(blacklistingSecret)).header("Secret", "password").get("/greeting");

        assertThat(log.toString()).contains("Secret=[ BLACKLISTED ]").doesNotContain("password");
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
