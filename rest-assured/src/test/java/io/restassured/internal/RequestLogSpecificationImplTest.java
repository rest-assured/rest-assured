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

import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.internal.log.LogRepository;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.RequestLogSpecification;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestLogSpecificationImplTest {

    private final PrintStream stream = new PrintStream(new ByteArrayOutputStream(), true);
    // defaultStream(..) enables pretty printing, so it goes first
    private final LogConfig logConfig = LogConfig.logConfig().defaultStream(stream);

    @Test
    void log_creates_a_request_log_specification_with_the_request_specification_log_repository_and_blacklisted_headers() {
        RequestSpecification spec = given().config(RestAssuredConfig.config().logConfig(LogConfig.logConfig().blacklistHeader("Secret")));

        RequestLogSpecificationImpl log = (RequestLogSpecificationImpl) spec.log();

        assertThat(log.getRequestSpecification()).isSameAs(spec);
        assertThat(log.getLogRepository()).isNotNull();
        assertThat(log.getBlacklistedHeaders()).containsExactly("Secret");
    }

    @Test
    void properties_can_be_set_and_read() {
        RequestLogSpecificationImpl log = new RequestLogSpecificationImpl();
        RequestSpecification spec = given();
        LogRepository logRepository = new LogRepository();
        Set<String> blacklistedHeaders = Set.of("a");

        log.setRequestSpecification(spec);
        log.setLogRepository(logRepository);
        log.setBlacklistedHeaders(blacklistedHeaders);

        assertThat(log.getRequestSpecification()).isSameAs(spec);
        assertThat(log.getLogRepository()).isSameAs(logRepository);
        assertThat(log.getBlacklistedHeaders()).isSameAs(blacklistedHeaders);
    }

    @Test
    void each_method_adds_a_request_logging_filter_with_its_log_detail_and_the_configured_pretty_printing() {
        assertLogs(RequestLogSpecification::params, LogDetail.PARAMS, true);
        assertLogs(RequestLogSpecification::parameters, LogDetail.PARAMS, true);
        assertLogs(RequestLogSpecification::uri, LogDetail.URI, true);
        assertLogs(RequestLogSpecification::method, LogDetail.METHOD, true);
        assertLogs(RequestLogSpecification::body, LogDetail.BODY, true);
        assertLogs(RequestLogSpecification::all, LogDetail.ALL, true);
        assertLogs(RequestLogSpecification::everything, LogDetail.ALL, true);
        assertLogs(RequestLogSpecification::headers, LogDetail.HEADERS, true);
        assertLogs(RequestLogSpecification::cookies, LogDetail.COOKIES, true);
    }

    @Test
    void pretty_printing_can_be_disabled_in_the_config() {
        LogConfig notPretty = logConfig.enablePrettyPrinting(false);

        assertLogs(notPretty, RequestLogSpecification::body, LogDetail.BODY, false);
        assertLogs(notPretty, RequestLogSpecification::all, LogDetail.ALL, false);
        assertLogs(notPretty, RequestLogSpecification::uri, LogDetail.URI, false);
    }

    @Test
    void pretty_printing_can_be_given_explicitly() {
        LogConfig prettyByDefault = logConfig;
        LogConfig notPrettyByDefault = logConfig.enablePrettyPrinting(false);

        assertLogs(prettyByDefault, log -> log.body(false), LogDetail.BODY, false);
        assertLogs(notPrettyByDefault, log -> log.body(true), LogDetail.BODY, true);
        assertLogs(prettyByDefault, log -> log.all(false), LogDetail.ALL, false);
        assertLogs(notPrettyByDefault, log -> log.everything(true), LogDetail.ALL, true);
    }

    @Test
    void the_filter_uses_the_url_encoding_and_blacklisted_headers_of_the_config() {
        LogConfig config = logConfig.urlEncodeRequestUri(false).blacklistHeader("Secret");
        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().config(RestAssuredConfig.config().logConfig(config));

        spec.log().uri();

        RequestLoggingFilter filter = lastLoggingFilter(spec);
        assertThat(field(filter, "showUrlEncodedUri")).isEqualTo(false);
        assertThat(field(filter, "blacklistedHeaders")).isEqualTo(Set.of("Secret"));
        assertThat(field(filter, "stream")).isSameAs(stream);
    }

    @Test
    void if_validation_fails_logs_to_a_stream_registered_in_the_log_repository() throws Exception {
        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().config(RestAssuredConfig.config().logConfig(logConfig));
        RequestLogSpecificationImpl log = (RequestLogSpecificationImpl) spec.log();

        RequestSpecification returned = log.ifValidationFails();

        assertThat(returned).isSameAs(spec);
        RequestLoggingFilter filter = lastLoggingFilter(spec);
        assertThat(field(filter, "logDetail")).isEqualTo(LogDetail.ALL);
        assertThat(field(filter, "shouldPrettyPrint")).isEqualTo(true);
        PrintStream filterStream = (PrintStream) field(filter, "stream");
        assertThat(filterStream).isNotSameAs(stream);
        filterStream.print("logged");
        filterStream.flush();
        assertThat(log.getLogRepository().getRequestLog()).isEqualTo("logged");
    }

    @Test
    void if_validation_fails_uses_the_given_log_detail_and_pretty_printing() {
        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().config(RestAssuredConfig.config().logConfig(LogConfig.logConfig().enablePrettyPrinting(false)));

        spec.log().ifValidationFails(LogDetail.HEADERS);
        RequestLoggingFilter first = lastLoggingFilter(spec);
        spec.log().ifValidationFails(LogDetail.BODY, true);
        RequestLoggingFilter second = lastLoggingFilter(spec);

        assertThat(field(first, "logDetail")).isEqualTo(LogDetail.HEADERS);
        assertThat(field(first, "shouldPrettyPrint")).isEqualTo(false);
        assertThat(field(second, "logDetail")).isEqualTo(LogDetail.BODY);
        assertThat(field(second, "shouldPrettyPrint")).isEqualTo(true);
    }

    @Test
    void if_validation_fails_without_log_repository_fails_with_null_pointer_exception() {
        RequestLogSpecificationImpl log = new RequestLogSpecificationImpl();
        log.setRequestSpecification(given());

        assertThatThrownBy(() -> log.ifValidationFails(LogDetail.ALL, true)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void logging_without_request_specification_fails() {
        RequestLogSpecificationImpl log = new RequestLogSpecificationImpl();

        assertThatThrownBy(log::all)
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot configure logging since request specification is not defined. You may be misusing the API.");
    }

    private void assertLogs(Function<RequestLogSpecification, RequestSpecification> method, LogDetail logDetail, boolean prettyPrint) {
        assertLogs(logConfig, method, logDetail, prettyPrint);
    }

    private void assertLogs(LogConfig logConfig, Function<RequestLogSpecification, RequestSpecification> method, LogDetail logDetail, boolean prettyPrint) {
        FilterableRequestSpecification spec = (FilterableRequestSpecification) given().config(RestAssuredConfig.config().logConfig(logConfig));
        int filtersBefore = spec.getDefinedFilters().size();

        RequestSpecification returned = method.apply(spec.log());

        assertThat(returned).isSameAs(spec);
        assertThat(spec.getDefinedFilters()).hasSize(filtersBefore + 1);
        RequestLoggingFilter filter = lastLoggingFilter(spec);
        assertThat(field(filter, "logDetail")).as("log detail").isEqualTo(logDetail);
        assertThat(field(filter, "shouldPrettyPrint")).as("pretty print").isEqualTo(prettyPrint);
        assertThat(field(filter, "stream")).isSameAs(stream);
        assertThat(field(filter, "showUrlEncodedUri")).isEqualTo(true);
        assertThat(field(filter, "blacklistedHeaders")).isEqualTo(logConfig.blacklistedHeaders());
    }

    private static RequestLoggingFilter lastLoggingFilter(FilterableRequestSpecification spec) {
        List<Filter> filters = spec.getDefinedFilters();
        Filter filter = filters.get(filters.size() - 1);
        assertThat(filter).isExactlyInstanceOf(RequestLoggingFilter.class);
        return (RequestLoggingFilter) filter;
    }

    private static Object field(Object object, String name) {
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(object);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
