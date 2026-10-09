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

package io.restassured.internal;

import io.restassured.RestAssured;
import io.restassured.authentication.BasicAuthScheme;
import io.restassured.authentication.ExplicitNoAuthScheme;
import io.restassured.authentication.NoAuthScheme;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.HeaderConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Header;
import io.restassured.internal.filter.FormAuthFilter;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.ProxySpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.spi.AuthFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HeaderConfig.headerConfig;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static io.restassured.config.SessionConfig.DEFAULT_SESSION_ID_NAME;
import static io.restassured.config.SessionConfig.sessionConfig;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

/**
 * Pins what {@link SpecificationMerger} overwrites and what it merges, for request and response specifications.
 */
class SpecificationMergerTest {

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }

    // Response specifications

    @Test
    void mergesCookies() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().expectCookie("first", "value1"));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().expectCookie("second", "value2"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getCookieAssertions()).hasSize(2);
    }

    @Test
    void mergesHeaders() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().expectHeader("first", "value1"));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().expectHeader("second", "value2"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getHeaderAssertions()).hasSize(2);
    }

    @Test
    void mergesBodyMatchers() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().expectBody("first", equalTo("value1")));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().expectBody("second", equalTo("value2")));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getBodyMatchers().size()).isEqualTo(2);
    }

    @Test
    void overwritesContentType() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().expectContentType(ContentType.ANY));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().expectContentType(ContentType.BINARY));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getExpectedContentType()).isEqualTo(ContentType.BINARY);
    }

    @Test
    void overwritesRootPath() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().rootPath("rootPath"));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().rootPath("new."));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getRootPath()).isEqualTo("new.");
    }

    @Test
    void overwritesStatusCode() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().expectStatusCode(200));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().expectStatusCode(400));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getStatusCode().matches(400)).isTrue();
    }

    @Test
    void overwritesStatusLine() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().expectStatusLine("something"));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().expectStatusLine("something else"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getStatusLine().matches("something else")).isTrue();
    }

    @Test
    void mergesResponseParsers() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().registerParser("some/xml", Parser.XML));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().registerParser("some/json", Parser.JSON));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getRpr().hasCustomParser("some/xml")).isTrue();
        assertThat(merge.getRpr().hasCustomParser("some/json")).isTrue();
    }

    @Test
    void a_parser_registered_for_the_same_content_type_is_overwritten() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().registerParser("some/thing", Parser.XML));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().registerParser("some/thing", Parser.JSON));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getRpr().getParser("some/thing")).isEqualTo(Parser.JSON);
    }

    @Test
    void overwritesDefaultParser() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().setDefaultParser(Parser.XML));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().setDefaultParser(Parser.JSON));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getRpr().getDefaultParser()).isEqualTo(Parser.JSON);
    }

    @Test
    void overwritesLogDetail() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().log(LogDetail.COOKIES));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().log(LogDetail.URI));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getLogDetail()).isEqualTo(LogDetail.URI);
    }

    @Test
    void overwritesResponseTime() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().expectResponseTime(lessThan(1L), TimeUnit.SECONDS));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder().expectResponseTime(lessThan(2L), TimeUnit.MINUTES));
        SpecificationMerger.merge(merge, with);
        Map.Entry<?, ?> responseTime = merge.getExpectedResponseTime();
        assertThat(responseTime.getValue()).isEqualTo(TimeUnit.MINUTES);
        assertThat(responseTime.getKey().toString()).isEqualTo(lessThan(2L).toString());
    }

    @Test
    void values_that_the_merged_response_specification_does_not_define_are_overwritten_with_null() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder().expectContentType(ContentType.JSON).rootPath("root")
                .expectStatusCode(200).expectStatusLine("line").expectResponseTime(lessThan(1L)).setDefaultParser(Parser.XML)
                .log(LogDetail.ALL).registerParser("some/xml", Parser.XML).expectHeader("h", "v").expectCookie("c", "v")
                .expectBody("a", equalTo("b")));
        ResponseSpecificationImpl with = response(new ResponseSpecBuilder());
        SpecificationMerger.merge(merge, with);

        assertThat(merge.getExpectedContentType()).isNull();
        assertThat(merge.getRootPath()).isEmpty();
        assertThat(merge.getStatusCode()).isNull();
        assertThat(merge.getStatusLine()).isNull();
        assertThat(merge.getExpectedResponseTime()).isNull();
        assertThat(merge.getRpr().getDefaultParser()).isNull();
        assertThat(merge.getLogDetail()).isNull();
        // Merged values are kept
        assertThat(merge.getRpr().hasCustomParser("some/xml")).isTrue();
        assertThat(merge.hasHeaderAssertions()).isTrue();
        assertThat(merge.hasCookieAssertions()).isTrue();
        assertThat(merge.getBodyMatchers().size()).isEqualTo(1);
    }

    @Test
    void merging_a_response_specification_with_null_is_not_allowed() {
        ResponseSpecificationImpl merge = response(new ResponseSpecBuilder());
        assertThatThrownBy(() -> SpecificationMerger.merge(merge, (ResponseSpecificationImpl) null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Specification to merge with cannot be null");
        assertThatThrownBy(() -> SpecificationMerger.merge(null, merge))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Specification to merge cannot be null");
    }

    // Request specifications

    @Test
    void mergesFilters() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addFilter(newFilter()).addFilter(newFilter()));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().addFilter(newFilter()).addFilters(asList(newFilter(), newFilter())));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getDefinedFilters()).hasSize(5);
    }

    @Test
    void filters_of_the_merged_specification_are_appended_in_order_without_those_already_present() {
        Filter a = newFilter(), b = newFilter(), c = newFilter(), d = newFilter();
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addFilters(asList(a, b)));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().addFilters(asList(c, b, d, c)));
        SpecificationMerger.merge(merge, with);
        // A filter that is in the merged specification twice is added twice
        assertThat(merge.getDefinedFilters()).containsExactly(a, b, c, d, c);
    }

    @Test
    void sameFilterNotAddedTwice() {
        Filter filter = newFilter();
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addFilter(filter));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().addFilter(filter));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getDefinedFilters()).hasSize(1);
    }

    @Test
    void authFiltersAreOverwritten() {
        FormAuthFilter first = new FormAuthFilter();
        FormAuthFilter second = new FormAuthFilter();
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addFilter(first));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().addFilter(second));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getDefinedFilters()).containsExactly(second);
    }

    @Test
    void auth_filters_are_kept_when_the_merged_specification_has_none() {
        Filter other = newFilter();
        AuthFilter authFilter = newAuthFilter();
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addFilters(asList(authFilter, other)));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().addFilter(newFilter()));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getDefinedFilters()).hasSize(3).startsWith(authFilter, other);
    }

    @Test
    void only_auth_filters_are_removed_when_both_specifications_have_auth_filters() {
        Filter first = newFilter();
        Filter second = newFilter();
        AuthFilter firstAuth = newAuthFilter();
        AuthFilter secondAuth = newAuthFilter();
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addFilters(asList(firstAuth, first)));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().addFilters(asList(second, secondAuth)));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getDefinedFilters()).containsExactly(first, second, secondAuth);
    }

    @Test
    void authFiltersAreRemovedIfMergedSpecContainsExplicitNoAuth() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addFilter(new FormAuthFilter()));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setAuth(new ExplicitNoAuthScheme()));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getDefinedFilters()).isEmpty();
    }

    @Test
    void auth_filters_of_a_specification_with_explicit_no_auth_are_still_added() {
        AuthFilter authFilter = newAuthFilter();
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addFilter(new FormAuthFilter()));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setAuth(new ExplicitNoAuthScheme()).addFilter(authFilter));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getDefinedFilters()).containsExactly(authFilter);
    }

    @Test
    void overwritesUrlEncodingStatus() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setUrlEncodingEnabled(true));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setUrlEncodingEnabled(false));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.isUrlEncodingEnabled()).isFalse();

        SpecificationMerger.merge(merge, request(new RequestSpecBuilder().setUrlEncodingEnabled(true)));
        assertThat(merge.isUrlEncodingEnabled()).isTrue();
    }

    @Test
    void mergesMultiPartParams() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addMultiPart("controlName1", "fileName1", new byte[0]));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().addMultiPart("controlName2", "fileName2", new byte[0])
                .addMultiPart("controlName1", "fileName3", new byte[0]));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getMultiPartParams()).extracting(MultiPartSpecification::getFileName).containsExactly("fileName1", "fileName2", "fileName3");
    }

    @Test
    void restAssuredConfigurationIsOverwritten() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setConfig(new RestAssuredConfig()));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setConfig(newConfig().redirect(redirectConfig().allowCircularRedirects(false))));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getConfig().getRedirectConfig().allowsCircularRedirects()).isFalse();
    }

    @Test
    void the_config_of_the_merged_specification_is_used_as_is_when_this_config_is_not_user_configured() {
        RestAssuredConfig withConfig = newConfig().redirect(redirectConfig().maxRedirects(5));
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setConfig(new RestAssuredConfig()));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setConfig(withConfig));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getConfig()).isSameAs(withConfig);
    }

    @Test
    void the_config_of_the_merged_specification_is_used_when_this_specification_has_no_config() {
        RestAssuredConfig withConfig = newConfig().redirect(redirectConfig().maxRedirects(5));
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setConfig(null));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setConfig(withConfig));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getConfig()).isSameAs(withConfig);
    }

    @Test
    void this_config_is_kept_when_the_config_of_the_merged_specification_is_not_user_configured() {
        RestAssuredConfig mergeConfig = newConfig().redirect(redirectConfig().maxRedirects(5));
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setConfig(mergeConfig));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setConfig(new RestAssuredConfig()));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getConfig()).isSameAs(mergeConfig);

        RequestSpecificationImpl mergeWithoutConfig = request(new RequestSpecBuilder().setConfig(null));
        SpecificationMerger.merge(mergeWithoutConfig, with);
        assertThat(mergeWithoutConfig.getConfig()).isNull();
    }

    @Test
    void user_configured_configs_are_merged_per_config_type_when_both_configs_are_user_configured() {
        RedirectConfig redirectConfig = redirectConfig().maxRedirects(5);
        LogConfig mergeLogConfig = logConfig().enablePrettyPrinting(false);
        LogConfig withLogConfig = logConfig().urlEncodeRequestUri(false);
        HeaderConfig headerConfig = headerConfig().overwriteHeadersWithName("X");
        RestAssuredConfig mergeConfig = newConfig().redirect(redirectConfig).logConfig(mergeLogConfig);
        RestAssuredConfig withConfig = newConfig().logConfig(withLogConfig).headerConfig(headerConfig);
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setConfig(mergeConfig));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setConfig(withConfig));

        SpecificationMerger.merge(merge, with);

        RestAssuredConfig merged = merge.getConfig();
        assertThat(merged).isNotSameAs(mergeConfig).isNotSameAs(withConfig);
        assertThat(merged.getRedirectConfig()).isSameAs(redirectConfig);
        assertThat(merged.getLogConfig()).isSameAs(withLogConfig);
        assertThat(merged.getHeaderConfig()).isSameAs(headerConfig);
        // Configs that neither specification configured are taken from this specification
        assertThat(merged.getEncoderConfig()).isSameAs(mergeConfig.getEncoderConfig());
        assertThat(merged.getSSLConfig()).isSameAs(mergeConfig.getSSLConfig());
        assertThat(merged.getCsrfConfig()).isSameAs(mergeConfig.getCsrfConfig());
    }

    @Test
    void every_config_type_is_merged() throws Exception {
        // SpecificationMerger merges each config of RestAssuredConfig by type, update it when a config type is added
        Field configs = RestAssuredConfig.class.getDeclaredField("configs");
        configs.setAccessible(true);
        assertThat((Map<?, ?>) configs.get(new RestAssuredConfig())).hasSize(18);
    }

    @Test
    void mergesRequestCookies() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addCookie("first", "value1").addCookie("same", "1"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().addCookie("second", "value2").addCookie("same", "2"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getCookies().asList()).extracting(Cookie::toString).containsExactly("first=value1", "same=1", "second=value2", "same=2");
    }

    @Test
    void overwritesSessionId() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setSessionId("value1"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setSessionId("value2"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getCookies().get(DEFAULT_SESSION_ID_NAME).getValue()).isEqualTo("value2");
        assertThat(merge.getCookies().getList(DEFAULT_SESSION_ID_NAME)).hasSize(1);
    }

    @Test
    void the_session_id_is_removed_case_insensitively_and_other_cookies_keep_their_order() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addCookie("a", "1").addCookie("jsessionid", "value1").addCookie("b", "2"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setSessionId("value2"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getCookies().asList()).extracting(Cookie::toString).containsExactly("a=1", "b=2", "JSESSIONID=value2");
    }

    @Test
    void overwritesSessionIdWhenMergingMultipleTimes() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setSessionId("value1"));
        RequestSpecificationImpl with1 = request(new RequestSpecBuilder().setSessionId("value2"));
        RequestSpecificationImpl with2 = request(new RequestSpecBuilder().setSessionId("value3"));
        SpecificationMerger.merge(merge, with1);
        assertThat(merge.getCookies().get(DEFAULT_SESSION_ID_NAME).getValue()).isEqualTo("value2");
        SpecificationMerger.merge(merge, with2);
        assertThat(merge.getCookies().get(DEFAULT_SESSION_ID_NAME).getValue()).isEqualTo("value3");
    }

    @Test
    void overwritesSessionIdWhenDefinedInConfig() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setConfig(newConfig().sessionConfig(sessionConfig().sessionIdName("ikk"))).setSessionId("ikk", "value1"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setConfig(newConfig().sessionConfig(sessionConfig().sessionIdName("ikk2"))).setSessionId("ikk2", "value2"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getCookies().hasCookieWithName("ikk")).isFalse();
        assertThat(merge.getCookies().get("ikk2").getValue()).isEqualTo("value2");
    }

    @Test
    void the_session_id_name_of_this_config_is_used_when_the_merged_specification_has_no_config() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setConfig(newConfig().sessionConfig(sessionConfig().sessionIdName("ikk"))).setSessionId("ikk", "value1"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setConfig(null).addCookie("ikk", "value2"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getCookies().getList("ikk")).extracting(Cookie::getValue).containsExactly("value2");
    }

    @Test
    void the_session_id_is_kept_when_the_merged_specification_has_no_cookie_with_its_session_id_name() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setSessionId("value1"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setConfig(newConfig().sessionConfig(sessionConfig().sessionIdName("other"))).addCookie(DEFAULT_SESSION_ID_NAME, "value2"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getCookies().getList(DEFAULT_SESSION_ID_NAME)).extracting(Cookie::getValue).containsExactly("value1", "value2");
    }

    @Test
    void copiesSessionIdWhenFirstRequestSpecBuilderDoesntHaveASessionIdSpecified() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder());
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setConfig(newConfig().sessionConfig(sessionConfig().sessionIdName("ikk2"))).setSessionId("ikk2", "value2"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getCookies().get("ikk2").getValue()).isEqualTo("value2");
    }

    @Test
    void doesntOverwriteSessionIdFromMergingSpecWhenItDoesntHaveASessionIdSpecified() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setConfig(newConfig().sessionConfig(sessionConfig().sessionIdName("ikk2"))).setSessionId("ikk2", "value2"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder());
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getCookies().get("ikk2").getValue()).isEqualTo("value2");
    }

    @Test
    void mergeRequestSpecsOverrideBaseUri() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setBaseUri("http://www.exampleSpec.com"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setBaseUri("http://www.exampleSpec2.com"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getBaseUri()).isEqualTo("http://www.exampleSpec2.com");
    }

    @Test
    void mergeRequestSpecsOverrideBasePath() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setBasePath("http://www.exampleSpec.com"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setBasePath("http://www.exampleSpec2.com"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getBasePath()).isEqualTo("http://www.exampleSpec2.com");
    }

    @Test
    void mergeRequestSpecsOverrideProxySpecification() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setProxy("127.0.0.1"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setProxy("localhost"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.getProxySpecification().getHost()).isEqualTo("localhost");
    }

    @Test
    void mergeRequestSpecsOverrideAllowContentType() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setContentType("content-type"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().noContentType());
        SpecificationMerger.merge(merge, with);
        assertThat(merge.isAllowContentType()).isFalse();
    }

    @Test
    void mergeRequestSpecsOverrideAddCsrfFilter() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder());
        RequestSpecificationImpl with = request(new RequestSpecBuilder().disableCsrf());
        SpecificationMerger.merge(merge, with);
        assertThat(merge.isAddCsrfFilter()).isFalse();

        SpecificationMerger.merge(merge, request(new RequestSpecBuilder()));
        assertThat(merge.isAddCsrfFilter()).isTrue();
    }

    @Test
    void mergeRequestSpecsOverrideContentTypeWhenDisallowContentTypeOnOriginal() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().noContentType());
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setContentType("content-type"));
        SpecificationMerger.merge(merge, with);
        assertThat(merge.isAllowContentType()).isTrue();
        assertThat(merge.getContentType()).isEqualTo("content-type");
    }

    @Test
    void values_that_the_merged_request_specification_does_not_define_overwrite_this_specification() {
        ProxySpecification proxy = ProxySpecification.host("proxy");
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setBaseUri("http://base").setBasePath("/basePath").setPort(1234)
                .setAuth(new BasicAuthScheme()).setBody("body").setProxy(proxy).setUrlEncodingEnabled(false).noContentType().disableCsrf());
        merge.path("/path");
        merge.setMethod("PUT");
        RequestSpecificationImpl with = request(new RequestSpecBuilder());

        SpecificationMerger.merge(merge, with);

        assertThat(merge.getBaseUri()).isEqualTo(RestAssured.DEFAULT_URI);
        assertThat(merge.getBasePath()).isEqualTo(RestAssured.DEFAULT_PATH);
        assertThat(merge.getRequestPort()).isEqualTo(RestAssured.UNDEFINED_PORT);
        assertThat(merge.getAuthenticationScheme()).isInstanceOf(NoAuthScheme.class);
        assertThat((Object) merge.getBody()).isNull();
        assertThat(merge.getProxySpecification()).isNull();
        assertThat(merge.isUrlEncodingEnabled()).isTrue();
        assertThat(merge.isAllowContentType()).isTrue();
        assertThat(merge.isAddCsrfFilter()).isTrue();
        assertThat(merge.getMethod()).isNull();
        assertThat(merge.getUserDefinedPath()).isEmpty();
    }

    @Test
    void scalar_values_of_the_merged_request_specification_overwrite_this_specification() {
        ProxySpecification proxy = ProxySpecification.host("proxy");
        BasicAuthScheme auth = new BasicAuthScheme();
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().setBaseUri("http://base").setBasePath("/basePath").setPort(1234).setBody("body"));
        merge.path("/path");
        merge.setMethod("PUT");
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setBaseUri("http://other").setBasePath("/otherPath").setPort(5678)
                .setAuth(auth).setBody("other body").setProxy(proxy));
        with.path("/otherPath/{x}");
        with.setMethod("post");

        SpecificationMerger.merge(merge, with);

        assertThat(merge.getRequestPort()).isEqualTo(5678);
        assertThat(merge.getAuthenticationScheme()).isSameAs(auth);
        assertThat((Object) merge.getBody()).isEqualTo("other body");
        assertThat(merge.getProxySpecification()).isSameAs(proxy);
        assertThat(merge.getMethod()).isEqualTo("POST");
        assertThat(merge.getUserDefinedPath()).isEqualTo("/otherPath/{x}");
        assertThat(merge.getURI()).isEqualTo("http://other:5678/otherPath/otherPath/%7Bx%7D");
    }

    @Test
    void parameters_are_merged_and_parameters_of_the_merged_specification_overwrite_parameters_with_the_same_name() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder()
                .addParam("a", "1").addParam("b", "2").addQueryParam("qa", "1").addQueryParam("qb", "2")
                .addFormParam("fa", "1").addFormParam("fb", "2").addPathParam("pa", "1").addPathParam("pb", "2"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder()
                .addParam("b", "3", "4").addParam("c", "5").addQueryParam("qb", "3").addQueryParam("qc").addFormParam("fb", "3")
                .addFormParam("fc", "4").addPathParam("pb", "3").addPathParam("pc", "4"));

        SpecificationMerger.merge(merge, with);

        assertThat(asStrings(merge.getRequestParams())).containsExactly(entry("a", "1"), entry("b", "[3, 4]"), entry("c", "5"));
        assertThat(asStrings(merge.getQueryParams())).containsExactly(entry("qa", "1"), entry("qb", "3"), entry("qc", "NoParameterValue"));
        assertThat(asStrings(merge.getFormParams())).containsExactly(entry("fa", "1"), entry("fb", "3"), entry("fc", "4"));
        assertThat(asStrings(merge.getNamedPathParams())).containsExactly(entry("pa", "1"), entry("pb", "3"), entry("pc", "4"));
    }

    @Test
    void headers_are_merged_after_the_config_so_that_the_merged_header_config_is_used() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addHeader("X", "1").addHeader("Y", "1").setContentType("text/plain"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().setConfig(newConfig().headerConfig(headerConfig().overwriteHeadersWithName("X")))
                .addHeader("X", "2").addHeader("Y", "2").setContentType("application/json"));

        SpecificationMerger.merge(merge, with);

        // The merged header config only overwrites X, so Content-Type (overwritten by default) is now kept twice
        assertThat(merge.getHeaders().asList()).extracting(Header::toString)
                .containsExactly("Y=1", "Content-Type=text/plain", "X=2", "Y=2", "Content-Type=application/json");
    }

    @Test
    void content_type_and_accept_headers_are_overwritten_by_default() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addHeader("X", "1").addHeader("Accept", "a").setContentType("text/plain"));
        RequestSpecificationImpl with = request(new RequestSpecBuilder().addHeader("X", "2").addHeader("accept", "b").setContentType("application/json"));

        SpecificationMerger.merge(merge, with);

        assertThat(merge.getHeaders().asList()).extracting(Header::toString)
                .containsExactly("X=1", "X=2", "accept=b", "Content-Type=application/json");
    }

    @Test
    void named_and_unnamed_path_params_of_the_merged_specification_become_named_path_params() {
        RequestSpecificationImpl with = request(new RequestSpecBuilder().addPathParam("n", "N"));
        with.path("/{n}/{a}/{b}");
        with.buildUnnamedPathParameterTuples("x", "y", "z");
        RequestSpecificationImpl merge = request(new RequestSpecBuilder().addPathParam("a", "A").addPathParam("o", "O"));

        SpecificationMerger.merge(merge, with);

        assertThat(merge.getNamedPathParams()).containsExactly(entry("a", "x"), entry("o", "O"), entry("n", "N"), entry("b", "y"));
    }

    // The merged specification kept the unnamed path param values as Strings where tuples were expected, so these failed with
    // a ClassCastException (a MissingPropertyException before RequestSpecificationImpl was written in Java)

    @Test
    void unnamed_path_params_of_the_merged_specification_are_copied() {
        RequestSpecificationImpl with = request(new RequestSpecBuilder());
        with.path("/{a}/{b}");
        with.buildUnnamedPathParameterTuples("x", "y", "z");
        RequestSpecificationImpl merge = request(new RequestSpecBuilder());

        SpecificationMerger.merge(merge, with);

        assertThat(merge.getUnnamedPathParamValues()).containsExactly("x", "y", "z");
        assertThat(merge.getUnnamedPathParams()).containsExactly(entry("a", "x"), entry("b", "y"));
        assertThat(merge.getURI()).isEqualTo(with.getURI()).isEqualTo("http://localhost:8080/x/y");
        assertThat(merge.getRedundantUnnamedPathParamValues()).containsExactly("z");
    }

    @Test
    void removing_an_unnamed_path_param_from_the_merged_specification_does_not_change_the_specification_it_was_merged_with() {
        RequestSpecificationImpl with = request(new RequestSpecBuilder());
        with.path("/{a}/{b}");
        with.buildUnnamedPathParameterTuples("x", "y");
        RequestSpecificationImpl merge = request(new RequestSpecBuilder());
        SpecificationMerger.merge(merge, with);

        merge.removeUnnamedPathParamByValue("x");

        assertThat(merge.getUnnamedPathParamValues()).containsExactly("y");
        assertThat(with.getUnnamedPathParamValues()).containsExactly("x", "y");
    }

    @Test
    void a_specification_with_unnamed_path_params_can_be_merged_twice_and_used_for_a_request_with_unnamed_path_params() {
        List<String> uris = new ArrayList<>();
        given().filter((requestSpec, responseSpec, ctx) -> {
            FilterableRequestSpecification copy = (FilterableRequestSpecification) given().spec(requestSpec);
            uris.add(copy.getURI());
            uris.add(String.join(",", copy.getUnnamedPathParamValues()));
            FilterableRequestSpecification copyOfCopy = (FilterableRequestSpecification) given().spec(copy);
            uris.add(copyOfCopy.getURI());
            return new ResponseBuilder().setStatusCode(200).setBody("").build();
        }).get("/{a}/{b}", "x", "y");

        assertThat(uris).containsExactly("http://localhost:8080/x/y", "x,y", "http://localhost:8080/x/y");

        RequestSpecificationImpl with = request(new RequestSpecBuilder());
        with.path("/{a}/{b}");
        with.buildUnnamedPathParameterTuples("x", "y");
        List<String> sent = new ArrayList<>();
        given().spec(with).filter((requestSpec, responseSpec, ctx) -> {
            sent.add(requestSpec.getURI());
            return new ResponseBuilder().setStatusCode(200).setBody("").build();
        }).get("/{a}/{b}", "x", "y");

        assertThat(sent).containsExactly("http://localhost:8080/x/y");
    }

    @Test
    void merging_a_request_specification_with_null_is_not_allowed() {
        RequestSpecificationImpl merge = request(new RequestSpecBuilder());
        assertThatThrownBy(() -> SpecificationMerger.merge(merge, (RequestSpecificationImpl) null))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Specification to merge with cannot be null");
        assertThatThrownBy(() -> SpecificationMerger.merge(null, merge))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Specification to merge cannot be null");
    }

    private static RequestSpecificationImpl request(RequestSpecBuilder builder) {
        RequestSpecification spec = builder.build();
        return (RequestSpecificationImpl) spec;
    }

    private static ResponseSpecificationImpl response(ResponseSpecBuilder builder) {
        return (ResponseSpecificationImpl) builder.build();
    }

    private static Map<String, String> asStrings(Map<String, ?> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() instanceof NoParameterValue ? "NoParameterValue" : String.valueOf(e.getValue()),
                (a, b) -> a, LinkedHashMap::new));
    }

    // Anonymous classes since a non-capturing lambda may be the same instance every time
    private static Filter newFilter() {
        return new Filter() {
            @Override
            public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                return ctx.next(requestSpec, responseSpec);
            }
        };
    }

    private static AuthFilter newAuthFilter() {
        return new AuthFilter() {
            @Override
            public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                return ctx.next(requestSpec, responseSpec);
            }
        };
    }
}
