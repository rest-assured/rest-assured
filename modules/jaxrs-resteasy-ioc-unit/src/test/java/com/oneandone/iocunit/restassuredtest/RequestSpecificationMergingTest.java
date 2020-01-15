package com.oneandone.iocunit.restassuredtest;

import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.config.SessionConfig.sessionConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.equalTo;

import java.io.PrintStream;
import java.io.StringWriter;

import org.apache.commons.io.output.WriterOutputStream;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.GreetingResource;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.http.Header;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.specification.RequestSpecification;

/**
 * @author aschoerk
 */
@RunWith(IocUnitRunner.class)
@SutClasses({GreetingResource.class})
public class RequestSpecificationMergingTest {
    @Test
    public void
    query_params_are_merged() {
        // Given
        RequestSpecification specToMerge = new RequestSpecBuilder().addQueryParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().queryParam("param2", "value2").spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getQueryParams()).containsOnly(entry("param1", "value1"), entry("param2", "value2"));
    }

    @Test public void
    form_params_are_merged() {
        // Given
        RequestSpecification specToMerge = new RequestSpecBuilder().addFormParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().formParam("param2", "value2").spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getFormParams()).containsOnly(entry("param1", "value1"), entry("param2", "value2"));
    }

    @Test public void
    params_are_merged() {
        // Given
        RequestSpecification specToMerge = new RequestSpecBuilder().addParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().param("param2", "value2").spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getRequestParams()).containsOnly(entry("param1", "value1"), entry("param2", "value2"));
    }

    /*
    @Test public void
    attributes_are_merged() {
        // Given
        RequestSpecification specToMerge = new RequestSpecBuilder().addAttribute("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().attribute("param2", "value2").spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getAttributes()).containsOnly(entry("param1", "value1"), entry("param2", "value2"));
    }
    */

    @Test public void
    multi_parts_are_merged() {
        // Given
        RequestSpecification specToMerge = new RequestSpecBuilder().addMultiPart("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().multiPart("param2", "value2").spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getMultiPartParams()).hasSize(2);
    }


    @Test public void
    request_body_is_overwritten_when_defined_in_specification() {
        // Given
        RequestSpecification specToMerge = new RequestSpecBuilder().setBody("body2").build();

        // When
        RequestSpecification spec = RestAssured.given().body("body1").spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getBody()).isEqualTo("body2");
    }

/*
    @Test public void
    request_body_is_not_overwritten_when_not_defined_in_specification() {
        // Given
        RequestSpecification specToMerge = new RequestSpecBuilder().addQueryParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().body("body1").spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getBody()).isEqualTo("body1");
        Assertions.assertThat(implOf(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
    }
*/

    @Test public void
    base_path_is_overwritten_when_defined_in_specification() {
        // Given
        RestAssured.basePath = "/something";
        RequestSpecification specToMerge = new RequestSpecBuilder().setBasePath("basePath").build();

        // When
        RequestSpecification spec = RestAssured.given().body("body1").spec(specToMerge);

        // Then
        RestAssured.reset();
        Assertions.assertThat(implOf(spec).getBasePath()).isEqualTo("basePath");
    }


    @Test public void
    base_path_is_not_overwritten_when_not_defined_in_specification() {
        // Given
        RequestSpecification specToMerge = new RequestSpecBuilder().addQueryParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().body("body1").spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getBasePath()).isEqualTo(RestAssured.basePath);
        Assertions.assertThat(implOf(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
    }


    /*
    @Test public void
    mock_mvc_instance_is_overwritten_when_defined_in_specification() {
        // Given
        MockMvc otherMockMvcInstance = MockMvcBuilders.standaloneSetup(new PostController()).build();
        MockMvc thisMockMvcInstance = MockMvcBuilders.standaloneSetup(new GreetingController()).build();

        RequestSpecification specToMerge = new RequestSpecBuilder().setMockMvc(otherMockMvcInstance).build();

        // When
        RequestSpecification spec = RestAssured.given().mockMvc(thisMockMvcInstance).spec(specToMerge);

        // Then
        Object mockMvc = Whitebox.getInternalState(implOf(spec).getMockMvcFactory(), "mockMvc"); // Don't change this to a one-liner, since then it won't work on JDK 11
        assertThat(mockMvc).isSameAs(otherMockMvcInstance);
    }


    @Test public void
    mock_mvc_factory_is_not_overwritten_when_not_defined_in_specification() {
        // Given
        MockMvc mockMvcInstance = MockMvcBuilders.standaloneSetup(new GreetingController()).build();
        RequestSpecification specToMerge = new RequestSpecBuilder().addQueryParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().mockMvc(mockMvcInstance).spec(specToMerge);

        // Then
        Object mockMvc = Whitebox.getInternalState(implOf(spec).getMockMvcFactory(), "mockMvc"); // Don't change this to a one-liner, since then it won't work on JDK 11
        assertThat(mockMvc).isSameAs(mockMvcInstance);
        Assertions.assertThat(implOf(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
    }
*/

    @Test public void
    cookies_are_merged_when_defined_in_specification() {
        // Given
        Cookie otherCookie = new Cookie.Builder("cookie1", "value1").build();
        Cookie thisCookie = new Cookie.Builder("cookie2", "value2").build();

        RequestSpecification specToMerge = new RequestSpecBuilder().addCookie(otherCookie).build();

        // When
        RequestSpecification spec = RestAssured.given().cookie(thisCookie).spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getCookies()).containsOnly(thisCookie, otherCookie);
    }

    @Test public void
    cookies_are_not_overwritten_when_not_defined_in_specification() {
        // Given
        Cookie thisCookie = new Cookie.Builder("cookie2", "value2").build();
        RequestSpecification specToMerge = new RequestSpecBuilder().addQueryParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().cookie(thisCookie).spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getCookies()).containsOnly(thisCookie);
        Assertions.assertThat(implOf(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
    }

    @Test public void
    content_type_is_overwritten_when_defined_in_specification() {
        // Given
        RequestSpecification specToMerge = new RequestSpecBuilder().setContentType(ContentType.JSON).build();

        // When
        RequestSpecification spec = RestAssured.given().contentType(ContentType.XML).spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getRequestContentType()).isEqualTo(ContentType.JSON.toString());
    }

    @Test public void
    content_type_is_not_overwritten_when_not_defined_in_specification() {
        // Given
        RequestSpecification specToMerge = new RequestSpecBuilder().addQueryParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().contentType(ContentType.XML).spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getRequestContentType()).isEqualTo(ContentType.XML.toString());
        Assertions.assertThat(implOf(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
    }

    @Test public void
    headers_are_merged_when_defined_in_specification() {
        // Given
        Header otherHeader = new Header("header1", "value1");
        Header thisHeader = new Header("header2", "value2");

        RequestSpecification specToMerge = new RequestSpecBuilder().addHeader("header1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().header(thisHeader).spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getHeaders()).containsOnly(thisHeader, otherHeader);
    }

    @Test public void
    headers_are_not_overwritten_when_not_defined_in_specification() {
        // Given
        Header thisHeader = new Header("cookie2", "value2");
        RequestSpecification specToMerge = new RequestSpecBuilder().addQueryParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().header(thisHeader).spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getHeaders()).containsOnly(thisHeader);
        Assertions.assertThat(implOf(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
    }

    @Test public void
    configs_of_same_type_are_overwritten_when_defined_in_specification() {
        // Given
        RestAssuredConfig otherConfig = new RestAssuredConfig().with().jsonConfig(jsonConfig().with().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        RestAssuredConfig thisConfig = new RestAssuredConfig().with().jsonConfig(jsonConfig().with().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE));
        RequestSpecification specToMerge = new RequestSpecBuilder().setConfig(otherConfig).build();

        // When
        RequestSpecification spec = RestAssured.given().config(thisConfig).spec(specToMerge);

        // Then
        assertThat(implOf(spec).getConfig().getJsonConfig().numberReturnType()).isEqualTo(JsonPathConfig.NumberReturnType.BIG_DECIMAL);
    }

    @Test public void
    config_is_not_overwritten_when_not_defined_in_specification() {
        // Given
        RestAssuredConfig thisConfig = new RestAssuredConfig().with().jsonConfig(jsonConfig().with().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE));
        RequestSpecification specToMerge = new RequestSpecBuilder().addQueryParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().config(thisConfig).spec(specToMerge);

        // Then

        // This assertion is commented out since for some reason it fails during the release process
//        assertThat(implOf(spec).getRestAssuredConfig()).isSameAs(thisConfig);
        Assertions.assertThat(implOf(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
        assertThat(implOf(spec).getConfig().getJsonConfig().numberReturnType()).isEqualTo(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE);
    }

/*
    @Test public void
    interception_is_overwritten_when_defined_in_specification() {
        // Given
        MockHttpServletRequestBuilderInterceptor otherInterceptor = requestBuilder -> {};

        MockHttpServletRequestBuilderInterceptor thisInterceptor = requestBuilder -> {};

        RequestSpecification specToMerge = new RequestSpecBuilder().setMockHttpServletRequestBuilderInterceptor(otherInterceptor).build();

        // When
        RequestSpecification spec = RestAssured.given().interceptor(thisInterceptor).spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getInterceptor()).isEqualTo(otherInterceptor);
    }

    @Test public void
    interception_is_not_overwritten_when_not_defined_in_specification() {
        // Given
        MockHttpServletRequestBuilderInterceptor thisInterceptor = requestBuilder -> {};
        RequestSpecification specToMerge = new RequestSpecBuilder().addQueryParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().interceptor(thisInterceptor).spec(specToMerge);

        // Then
        Assertions.assertThat(implOf(spec).getInterceptor()).isEqualTo(thisInterceptor);
        Assertions.assertThat(implOf(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
    }
*/
    @Test public void
    logging_is_overwritten_when_defined_in_specification() {
        // Given
        StringWriter writer = new StringWriter();
        PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        RequestSpecification specToMerge = new RequestSpecBuilder().setConfig(RestAssuredConfig.newConfig()
                .logConfig(LogConfig.logConfig().defaultStream(captor))).and().log(LogDetail.ALL).build();

        // When
        RestAssured.given().
                log().params().
                spec(specToMerge).
                when().
                get("/greeting?name={name}", "Johan").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));

        // Then
        assertThat(writer.toString()).isEqualTo(String.format("Request method:\tGET%n" +
                                                              "Request URI:\thttp://localhost:8080/greeting?name=Johan%n" +
                                                              "Proxy:\t\t\t<none>%n" +
                                                              "Request params:\t<none>%n" +
                                                              "Query params:\t<none>%n" +
                                                              "Form params:\t<none>%n" +
                                                              "Path params:\t<none>%n" +
                                                              // TODO: "Headers:\t\t<none>%n" +
                                                              "Headers:\t\tAccept=*/*%n" +
                                                              "Cookies:\t\t<none>%n" +
                                                              "Multiparts:\t\t<none>%n" +
                                                              "Body:\t\t\t<none>%n"));
    }


    @Test public void
    logging_is_not_overwritten_when_not_defined_in_specification() {
        // Given
        StringWriter writer = new StringWriter();
        PrintStream captor = new PrintStream(new WriterOutputStream(writer), true);
        RequestSpecification specToMerge =
                new RequestSpecBuilder()
                        .setConfig(RestAssuredConfig
                                .newConfig()
                                .logConfig(LogConfig.logConfig().defaultStream(captor))).
                addQueryParam("name", "Johan").build();

        // When
        RestAssured.given().
                spec(specToMerge).
                log().params().
                when().
                get("/greeting").
                then().
                body("id", equalTo(1)).
                body("content", equalTo("Hello, Johan!"));

        // Then
        assertThat(writer.toString()).isEqualTo(String.format("Request params:\t<none>%n" +
                                                              "Query params:\tname=Johan%n" +
                                                              "Form params:\t<none>%n" +
                                                              "Path params:\t<none>%n" +
                                                              "Multiparts:\t\t<none>%n"));
    }

    /*
    @Test public void
    authentication_is_overwritten_when_defined_in_specification() {
        // Given
        MockMvcAuthenticationScheme otherAuth = RestAssured.principal("other");
        MockMvcAuthenticationScheme thisAuth = RestAssured.principal("this");
        RequestSpecification specToMerge = new RequestSpecBuilder().setAuth(otherAuth).build();

        // When
        RequestSpecification spec = RestAssured.given().spec(new RequestSpecBuilder().setAuth(thisAuth).build()).spec(specToMerge);

        // Then
        assertThat(((TestingAuthenticationToken) implOf(spec).getAuthentication()).getPrincipal()).isEqualTo("other");
    }

    @Test public void
    authentication_is_overwritten_when_using_dsl_and_defined_in_specification() {
        // Given
        MockMvcAuthenticationScheme otherAuth = RestAssured.principal("other");
        RequestSpecification specToMerge = new RequestSpecBuilder().setAuth(otherAuth).build();

        // When
        RequestSpecification spec = RestAssured.given().auth().principal("this").and().spec(specToMerge);

        // Then
        assertThat(((TestingAuthenticationToken) implOf(spec).getAuthentication()).getPrincipal()).isEqualTo("other");
    }


    @Test public void
    authentication_is_not_overwritten_when_not_defined_in_specification() {
        // Given
        AuthenticationScheme thisAuth = RestAssured.principal("this");
        RequestSpecification specToMerge = new RequestSpecBuilder().addQueryParam("param1", "value1").build();

        // When
        RequestSpecification spec = RestAssured.given().spec(new RequestSpecBuilder().setAuth(thisAuth).build()).spec(specToMerge);

        // Then
        assertThat(((TestingAuthenticationToken) implOf(spec).getAuthentication()).getPrincipal()).isEqualTo("this");
        Assertions.assertThat(implOf(spec).getQueryParams()).containsOnly(entry("param1", "value1"));
    }
*/

    @Test public void
    configurations_are_merged() {
        // Given
        RestAssuredConfig cfg1 = new RestAssuredConfig().with().jsonConfig(jsonConfig().with().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE));
        RequestSpecification specToMerge = new RequestSpecBuilder().setConfig(cfg1).build();

        // When
        RestAssuredConfig cfg2 = new RestAssuredConfig().sessionConfig(sessionConfig().sessionIdName("php"));
        RequestSpecification spec = RestAssured.given().config(cfg2).spec(specToMerge);

        // Then
        RestAssuredConfig mergedConfig = implOf(spec).getConfig();
        assertThat(mergedConfig.getSessionConfig().sessionIdName()).isEqualTo("php");
        assertThat(mergedConfig.getJsonConfig().numberReturnType()).isEqualTo(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE);
    }


    private RequestSpecificationImpl implOf(RequestSpecification spec) {
        return (RequestSpecificationImpl) spec;
    }

}
