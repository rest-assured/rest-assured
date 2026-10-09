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

package io.restassured.internal.filter;

import io.restassured.RestAssured;
import io.restassured.authentication.FormAuthConfig;
import io.restassured.config.CsrfConfig;
import io.restassured.config.SessionConfig;
import io.restassured.filter.session.SessionFilter;
import io.restassured.internal.filter.RecordingServer.Reply;
import io.restassured.internal.filter.RecordingServer.Request;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RestAssuredConfig.config;
import static io.restassured.config.SessionConfig.sessionConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

/**
 * Pins the behavior of {@link FormAuthFilter} (including the named-argument construction in
 * {@code RequestSpecificationImpl}) against a real HTTP server.
 */
class FormAuthFilterTest {

    private static final String LOGIN_PAGE = "<html><head><title>Login</title></head><body>" +
            "<form action=\"%s\" method=\"POST\">" +
            "<input type=\"text\" name=\"user\"/>" +
            "<input type=\"password\" name=\"pass\"/>" +
            "%s" +
            "<input type=\"submit\" value=\"Login\"/>" +
            "</form></body></html>";

    private RecordingServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new RecordingServer();
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = server.port();
        server.route("GET /secured", securedOr(r -> Reply.html(loginPage("login", "")).withCookie("PAGE=p1")));
        server.route("POST /login", r -> Reply.text("logged in").withCookie("SESSION=s1"));
    }

    @AfterEach
    void stopServer() {
        server.close();
        RestAssured.reset();
    }

    @Test
    void parses_login_page_when_no_form_auth_config_is_given() {
        String body = given().auth().form("John", "Doe").when().get("/secured").then().statusCode(200).extract().asString();

        assertThat(body).isEqualTo("OK");
        assertThat(server.requestLines()).containsExactly("GET /secured", "POST /login", "GET /secured");
        Request login = server.lastRequestTo("POST /login");
        assertThat(login.body()).isEqualTo("user=John&pass=Doe");
        assertThat(login.cookies()).isEqualTo("PAGE=p1");
        assertThat(login.header("Content-Type")).startsWith("application/x-www-form-urlencoded");
        assertThat(server.lastRequestTo("GET /secured").cookies()).contains("SESSION=s1");
    }

    @Test
    void form_action_with_leading_slash_is_used_as_is() {
        server.route("GET /secured", securedOr(r -> Reply.html(loginPage("/login", ""))));

        given().auth().form("John", "Doe").when().get("/secured").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /secured", "POST /login", "GET /secured");
    }

    @Test
    void nested_form_action_without_leading_slash_gets_a_slash_prepended() {
        server.route("GET /secured", securedOr(r -> Reply.html(loginPage("auth/login", ""))));
        server.route("POST /auth/login", r -> Reply.text("logged in").withCookie("SESSION=s1"));

        given().auth().form("John", "Doe").when().get("/secured").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /secured", "POST /auth/login", "GET /secured");
    }

    @Test
    void does_not_fetch_login_page_when_form_auth_config_is_complete() {
        server.route("POST /j_check", r -> Reply.text("logged in").withCookie("SESSION=s1"));

        given().auth().form("John", "Doe", new FormAuthConfig("j_check", "u", "p")).when().get("/secured").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("POST /j_check", "GET /secured");
        Request login = server.lastRequestTo("POST /j_check");
        assertThat(login.body()).isEqualTo("u=John&p=Doe");
        assertThat(login.cookies()).isNull();
    }

    @Test
    void sends_additional_input_fields_read_from_login_page() {
        String extra = "<input type=\"hidden\" name=\"hidden1\" value=\"v1\"/><input type=\"hidden\" name=\"hidden2\" value=\"\"/>";
        server.route("GET /secured", securedOr(r -> Reply.html(loginPage("login", extra))));

        given().auth().form("John", "Doe", new FormAuthConfig("/login", "user", "pass").withAdditionalFields("hidden1", "hidden2")).
                when().get("/secured").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /secured", "POST /login", "GET /secured");
        assertThat(server.lastRequestTo("POST /login").body()).isEqualTo("user=John&pass=Doe&hidden1=v1&hidden2=");
    }

    @Test
    void fails_with_the_name_of_an_additional_input_field_that_is_missing_from_login_page() {
        server.route("GET /secured", securedOr(r -> Reply.html(loginPage("login", "<input type=\"hidden\" name=\"hidden1\" value=\"v1\"/>"))));

        assertThatThrownBy(() -> given().auth().form("John", "Doe", new FormAuthConfig("/login", "user", "pass").withAdditionalFields("hidden1", "missing")).
                when().get("/secured"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Couldn't find the additional input field \"missing\" on the login page. Check the additional fields specified in FormAuthConfig.");
        assertThat(server.requestLines()).containsExactly("GET /secured");
    }

    @Test
    void sends_additional_input_field_without_value_attribute_as_parameter_without_value() {
        // Before the Java port this failed with "IllegalArgumentException: parameterValues cannot be null"
        // because Groovy passed the null value as a null varargs array to formParam.
        server.route("GET /secured", securedOr(r -> Reply.html(loginPage("login", "<input type=\"hidden\" name=\"novalue\"/>"))));

        given().auth().form("John", "Doe", new FormAuthConfig("/login", "user", "pass").withAdditionalField("novalue")).
                when().get("/secured").then().statusCode(200);

        assertThat(server.lastRequestTo("POST /login").body()).isEqualTo("user=John&pass=Doe&novalue");
    }

    @Test
    void wraps_login_page_parse_failure_in_illegal_argument_exception() {
        server.route("GET /secured", securedOr(r -> Reply.text("no form here")));

        assertThatThrownBy(() -> given().auth().form("John", "Doe").when().get("/secured"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to parse login page. Check for errors on the login page or specify FormAuthConfig.")
                .hasCauseInstanceOf(Exception.class);
        assertThat(server.requestLines()).containsExactly("GET /secured");
    }

    @Test
    void follows_302_location_of_login_page_for_non_get_requests() {
        server.route("POST /secured-post", r -> hasSession(r) ? Reply.text("OK posted") : Reply.redirect("/login-page").withCookie("PAGE=p1"));
        server.route("GET /login-page", r -> Reply.html(loginPage("login", "")));

        String body = given().auth().form("John", "Doe").when().post("/secured-post").then().statusCode(200).extract().asString();

        assertThat(body).isEqualTo("OK posted");
        assertThat(server.requestLines()).containsExactly("POST /secured-post", "GET /login-page", "POST /login", "POST /secured-post");
        assertThat(server.lastRequestTo("GET /login-page").cookies()).isEqualTo("PAGE=p1");
        assertThat(server.lastRequestTo("POST /login").cookies()).isEqualTo("PAGE=p1");
    }

    @Test
    void fails_with_a_clear_message_when_login_page_is_redirected_without_location_header() {
        server.route("POST /secured-post", r -> new Reply(302, null, null, Map.of()));

        assertThatThrownBy(() -> given().auth().form("John", "Doe").when().post("/secured-post"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The request for the login page was redirected (302) without a Location header, so REST Assured couldn't follow the redirect to the login page.");
        assertThat(server.requestLines()).containsExactly("POST /secured-post");
    }

    @Test
    void posts_to_login_page_url_when_form_has_no_action() {
        String page = "<html><body><form method=\"POST\"><input type=\"text\" name=\"user\"/><input type=\"password\" name=\"pass\"/></form></body></html>";
        server.route("GET /secured", securedOr(r -> Reply.html(page)));
        server.route("POST /secured", r -> Reply.text("logged in").withCookie("SESSION=s1"));

        given().auth().form("John", "Doe").when().get("/secured").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /secured", "POST /secured", "GET /secured");
        assertThat(server.lastRequestTo("POST /secured").body()).isEqualTo("user=John&pass=Doe");
    }

    @Test
    void posts_to_login_page_url_when_form_action_is_empty() {
        server.route("GET /secured", securedOr(r -> Reply.html(loginPage("", ""))));
        server.route("POST /secured", r -> Reply.text("logged in").withCookie("SESSION=s1"));

        given().auth().form("John", "Doe").queryParam("q", "a b&c+d/\u00e9").when().get("/secured").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /secured", "POST /secured", "GET /secured");
        String loginQuery = server.lastRequestTo("POST /secured").query();
        assertThat(loginQuery).isEqualTo(server.requests.get(0).query());
        assertThat(URLDecoder.decode(loginQuery, UTF_8)).isEqualTo("q=a b&c+d/\u00e9");
    }

    @Test
    void posts_to_login_page_url_that_needs_url_encoding_when_form_has_no_action() {
        server.route("GET /secured/a b", securedOr(r -> Reply.html(loginPage("", ""))));
        server.route("POST /secured/a b", r -> Reply.text("logged in").withCookie("SESSION=s1"));

        given().auth().form("John", "Doe").when().get("/secured/{id}", "a b").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /secured/a b", "POST /secured/a b", "GET /secured/a b");
        assertThat(server.lastRequestTo("POST /secured/a b").rawPath()).isEqualTo("/secured/a%20b");
    }

    @Test
    void posts_to_url_encoded_path_of_form_action_without_encoding_it_again() {
        server.route("GET /secured", securedOr(r -> Reply.html(loginPage("/log%20in/a+b", ""))));
        server.route("POST /log in/a+b", r -> Reply.text("logged in").withCookie("SESSION=s1"));

        given().auth().form("John", "Doe").when().get("/secured").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /secured", "POST /log in/a+b", "GET /secured");
    }

    @Test
    void posts_url_encoded_query_of_form_action_without_encoding_it_again() {
        server.route("GET /secured", securedOr(r -> Reply.html(loginPage("/login?r=a%20b&s=%C3%A9%2B%26&flag", ""))));

        given().auth().form("John", "Doe").when().get("/secured").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /secured", "POST /login", "GET /secured");
        Request login = server.lastRequestTo("POST /login");
        assertThat(login.query()).doesNotContain("%25");
        assertThat(login.query().split("&")).extracting(it -> URLDecoder.decode(it, UTF_8)).containsExactly("r=a b", "s=\u00e9+&", "flag");
        assertThat(login.body()).isEqualTo("user=John&pass=Doe");
    }

    @Test
    void posts_to_redirected_login_page_url_when_form_has_no_action() {
        String page = "<html><body><form method=\"POST\"><input type=\"text\" name=\"user\"/><input type=\"password\" name=\"pass\"/></form></body></html>";
        server.route("POST /secured-post", r -> hasSession(r) ? Reply.text("OK posted") : Reply.redirect("/login-page?from=x"));
        server.route("GET /login-page", r -> Reply.html(page));
        server.route("POST /login-page", r -> Reply.text("logged in").withCookie("SESSION=s1"));

        given().auth().form("John", "Doe").when().post("/secured-post").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("POST /secured-post", "GET /login-page", "POST /login-page", "POST /secured-post");
        assertThat(server.lastRequestTo("POST /login-page").query()).isEqualTo("from=x");
    }

    @Test
    void posts_to_csrf_page_url_when_form_has_no_action() {
        String page = "<html><body><form method=\"POST\"><input type=\"text\" name=\"user\"/><input type=\"password\" name=\"pass\"/>" +
                "<input type=\"hidden\" name=\"_csrf\" value=\"tok\"/></form></body></html>";
        server.route("GET /csrf-page", r -> Reply.html(page));
        server.route("POST /csrf-page", r -> Reply.text("logged in").withCookie("SESSION=s1"));

        given().csrf("/csrf-page").auth().form("John", "Doe").when().get("/secured").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /csrf-page", "POST /csrf-page", "GET /secured");
        assertThat(server.lastRequestTo("POST /csrf-page").body()).isEqualTo("user=John&pass=Doe&_csrf=tok");
    }

    @Test
    void fetches_login_page_for_a_request_with_unnamed_path_parameters() {
        server.route("GET /secured/1", securedOr(r -> Reply.html(loginPage("login", "")).withCookie("PAGE=p1")));

        String body = given().auth().form("John", "Doe").when().get("/secured/{id}", 1).then().statusCode(200).extract().asString();

        assertThat(body).isEqualTo("OK");
        assertThat(server.requestLines()).containsExactly("GET /secured/1", "POST /login", "GET /secured/1");
    }

    @Test
    void fetches_login_page_for_a_request_with_named_path_parameters() {
        server.route("GET /secured/1", securedOr(r -> Reply.html(loginPage("login", "")).withCookie("PAGE=p1")));

        String body = given().auth().form("John", "Doe").pathParam("id", 1).when().get("/secured/{id}").then().statusCode(200).extract().asString();

        assertThat(body).isEqualTo("OK");
        assertThat(server.requestLines()).containsExactly("GET /secured/1", "POST /login", "GET /secured/1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a b", "\u00e9"})
    void fetches_login_page_once_url_encoded_for_an_unnamed_path_parameter_that_needs_url_encoding(String id) {
        assertFetchesLoginPageOnceEncoded(id, () -> given().auth().form("John", "Doe").when().get("/secured/{id}", id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a b", "\u00e9"})
    void fetches_login_page_once_url_encoded_for_a_named_path_parameter_that_needs_url_encoding(String id) {
        assertFetchesLoginPageOnceEncoded(id, () -> given().auth().form("John", "Doe").pathParam("id", id).when().get("/secured/{id}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a b", "\u00e9"})
    void fetches_login_page_once_url_encoded_for_a_path_that_needs_url_encoding(String id) {
        assertFetchesLoginPageOnceEncoded(id, () -> given().auth().form("John", "Doe").when().get("/secured/" + id));
    }

    @Test
    void fetches_login_page_with_query_parameters_of_request_url_encoded_once() {
        server.route("GET /secured/a b", securedOr(r -> Reply.html(loginPage("/login", ""))));

        given().auth().form("John", "Doe").queryParam("q", "a b&c").when().get("/secured/{id}?p=x y", "a b").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /secured/a b", "POST /login", "GET /secured/a b");
        Request loginPageRequest = server.requests.get(0);
        assertThat(loginPageRequest.rawPath()).isEqualTo("/secured/a%20b");
        assertThat(loginPageRequest.query()).isEqualTo(server.requests.get(2).query());
        assertThat(URLDecoder.decode(loginPageRequest.query(), UTF_8)).isEqualTo("q=a b&c&p=x y");
    }

    @Test
    void fetches_csrf_page_instead_of_requested_page_and_sends_form_token() {
        String csrfInput = "<input type=\"hidden\" name=\"_csrf\" value=\"tok\"/>";
        server.route("GET /csrf-page", r -> Reply.html(loginPage("login", csrfInput)).withCookie("CSRFPAGE=c1"));

        given().csrf("/csrf-page").cookie("mine", "m1").auth().form("John", "Doe").when().get("/secured").then().statusCode(200);

        assertThat(server.requestLines()).containsExactly("GET /csrf-page", "POST /login", "GET /secured");
        assertThat(server.lastRequestTo("GET /csrf-page").cookies()).isEqualTo("mine=m1");
        Request login = server.lastRequestTo("POST /login");
        assertThat(login.body()).isEqualTo("user=John&pass=Doe&_csrf=tok");
        assertThat(login.cookies()).isEqualTo("CSRFPAGE=c1");
        assertThat(login.header("X-CSRF-TOKEN")).isNull();
    }

    @Test
    void sends_csrf_token_from_meta_tag_as_header_on_login_request() {
        String page = "<html><head><meta name=\"_csrf_header\" content=\"htok\"/></head><body>" +
                "<form action=\"login\" method=\"POST\"><input type=\"text\" name=\"user\"/><input type=\"password\" name=\"pass\"/></form></body></html>";
        server.route("GET /csrf-page", r -> Reply.html(page));

        given().csrf("/csrf-page").auth().form("John", "Doe").when().get("/secured").then().statusCode(200);

        Request login = server.lastRequestTo("POST /login");
        assertThat(login.body()).isEqualTo("user=John&pass=Doe");
        assertThat(login.header("X-CSRF-TOKEN")).isEqualTo("htok");
    }

    @Test
    void applies_session_filter_of_original_request_to_login_request() {
        server.route("POST /login", r -> Reply.text("logged in").withCookie("MYSESSION=abc"));
        server.route("GET /secured", r -> r.cookies() != null && r.cookies().contains("MYSESSION=abc") ? Reply.text("OK") : Reply.text("denied"));
        SessionFilter sessionFilter = new SessionFilter();

        String body = given().
                config(config().sessionConfig(sessionConfig().sessionIdName("MYSESSION"))).
                filter(sessionFilter).
                auth().form("John", "Doe", new FormAuthConfig("/login", "user", "pass")).
        when().
                get("/secured").then().extract().asString();

        assertThat(body).isEqualTo("OK");
        assertThat(sessionFilter.getSessionId()).isEqualTo("abc");
    }

    @Test
    void applies_session_filter_subclass_of_original_request_to_login_request() {
        server.route("POST /login", r -> Reply.text("logged in").withCookie("MYSESSION=abc"));
        server.route("GET /secured", r -> r.cookies() != null && r.cookies().contains("MYSESSION=abc") ? Reply.text("OK") : Reply.text("denied"));
        SessionFilter sessionFilter = new SessionFilter() {
        };

        given().
                config(config().sessionConfig(sessionConfig().sessionIdName("MYSESSION"))).
                filter(sessionFilter).
                auth().form("John", "Doe", new FormAuthConfig("/login", "user", "pass")).
        when().
                get("/secured").then().body(equalTo("OK"));

        assertThat(sessionFilter.getSessionId()).isEqualTo("abc");
    }

    @Test
    void filter_used_directly_defaults_form_auth_config() {
        FormAuthFilter filter = new FormAuthFilter();
        filter.setUserName("John");
        filter.setPassword("Doe");
        filter.setCsrfConfig(new CsrfConfig());
        SessionConfig sessionConfig = new SessionConfig();
        filter.setSessionConfig(sessionConfig);
        assertThat(filter.getFormAuthConfig()).isNull();

        given().filter(filter).when().get("/secured").then().statusCode(200);

        assertThat(filter.getFormAuthConfig()).isNotNull();
        assertThat(filter.getFormAuthConfig().requiresParsingOfLoginPage()).isTrue();
        assertThat(filter.getUserName()).isEqualTo("John");
        assertThat(filter.getPassword()).isEqualTo("Doe");
        assertThat(filter.getSessionConfig()).isSameAs(sessionConfig);
        assertThat(server.lastRequestTo("POST /login").body()).isEqualTo("user=John&pass=Doe");
    }

    @Test
    void accessors_keep_their_types() throws NoSuchMethodException {
        assertThat(FormAuthFilter.class.getConstructor()).isNotNull();
        assertAccessor("UserName", Object.class);
        assertAccessor("Password", Object.class);
        assertAccessor("FormAuthConfig", FormAuthConfig.class);
        assertAccessor("SessionConfig", SessionConfig.class);
        assertAccessor("CsrfConfig", CsrfConfig.class);
    }

    private static void assertAccessor(String property, Class<?> type) throws NoSuchMethodException {
        Method getter = FormAuthFilter.class.getMethod("get" + property);
        assertThat(getter.getReturnType()).isEqualTo(type);
        assertThat(FormAuthFilter.class.getMethod("set" + property, type)).isNotNull();
    }

    private void assertFetchesLoginPageOnceEncoded(String id, Supplier<Response> request) {
        server.route("GET /secured/" + id, securedOr(r -> Reply.html(loginPage("/login", ""))));

        assertThat(request.get().then().statusCode(200).extract().asString()).isEqualTo("OK");

        assertThat(server.requestLines()).containsExactly("GET /secured/" + id, "POST /login", "GET /secured/" + id);
        String encodedPath = "/secured/" + URLEncoder.encode(id, UTF_8).replace("+", "%20");
        assertThat(server.requests.get(0).rawPath()).isEqualTo(encodedPath);
        assertThat(server.requests.get(2).rawPath()).isEqualTo(encodedPath);
    }

    private static Function<Request, Reply> securedOr(Function<Request, Reply> loginPage) {
        return r -> hasSession(r) ? Reply.text("OK") : loginPage.apply(r);
    }

    private static boolean hasSession(Request request) {
        return request.cookies() != null && request.cookies().contains("SESSION=s1");
    }

    private static String loginPage(String action, String extraInputs) {
        return String.format(LOGIN_PAGE, action, extraInputs);
    }
}
