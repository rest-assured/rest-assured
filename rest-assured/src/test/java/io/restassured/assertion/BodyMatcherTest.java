package io.restassured.assertion;

import io.restassured.builder.ResponseBuilder;
import io.restassured.config.MatcherConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.internal.ContentParser;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.assertion.BodyMatcher;
import io.restassured.response.Response;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class BodyMatcherTest {
    // Replace @Parameterized.Parameters with @MethodSource
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {MatcherConfig.ErrorDescriptionType.REST_ASSURED, 123, "123", true, ""},
                {MatcherConfig.ErrorDescriptionType.REST_ASSURED, 123, "123.0", false, "JSON path foo doesn't match.\nExpected: <123>\n  Actual: <123.0F>\n"},
                {MatcherConfig.ErrorDescriptionType.REST_ASSURED, 123L, "123", false, "JSON path foo doesn't match.\nExpected: <123L>\n  Actual: <123>\n"},
                {MatcherConfig.ErrorDescriptionType.REST_ASSURED, 123L, "123.0", false, "JSON path foo doesn't match.\nExpected: <123L>\n  Actual: <123.0F>\n"},
                {MatcherConfig.ErrorDescriptionType.REST_ASSURED, 3.14d, "3.14", false, "JSON path foo doesn't match.\nExpected: <3.14>\n  Actual: <3.14F>\n"},
                {MatcherConfig.ErrorDescriptionType.REST_ASSURED, 3.14f, "3.14", true, ""},
                {MatcherConfig.ErrorDescriptionType.REST_ASSURED, "3.14", "\"3.14\"", true, ""},
                {MatcherConfig.ErrorDescriptionType.REST_ASSURED, "3.14", "\"2.34\"", false, "JSON path foo doesn't match.\nExpected: 3.14\n  Actual: 2.34\n"},
                {MatcherConfig.ErrorDescriptionType.HAMCREST, 123, "123", true, ""},
                {MatcherConfig.ErrorDescriptionType.HAMCREST, 123, "123.0", false, "JSON path foo doesn't match.\n\nExpected: <123>\n  Actual: was <123.0F>"},
                {MatcherConfig.ErrorDescriptionType.HAMCREST, 123L, "123", false, "JSON path foo doesn't match.\n\nExpected: <123L>\n  Actual: was <123>"},
                {MatcherConfig.ErrorDescriptionType.HAMCREST, 123L, "123.0", false, "JSON path foo doesn't match.\n\nExpected: <123L>\n  Actual: was <123.0F>"},
                {MatcherConfig.ErrorDescriptionType.HAMCREST, 3.14d, "3.14", false, "JSON path foo doesn't match.\n\nExpected: <3.14>\n  Actual: was <3.14F>"},
                {MatcherConfig.ErrorDescriptionType.HAMCREST, 3.14f, "3.14", true, ""},
                {MatcherConfig.ErrorDescriptionType.HAMCREST, "3.14", "\"3.14\"", true, ""},
                {MatcherConfig.ErrorDescriptionType.HAMCREST, "3.14", "\"2.34\"", false, "JSON path foo doesn't match.\n\nExpected: \"3.14\"\n  Actual: was \"2.34\""},
        });
    }

    private final ResponseParserRegistrar responseParserRegistrar = new ResponseParserRegistrar();

    @DisplayName("expectedMessageFormatWithTypeDetails")
    @ParameterizedTest(name = "{index}: errorType={0}, isEqualValue={1}, jsonValue={2}, expectedSuccess={3}, expectedMessage={4}")
    @MethodSource("data")
    void expectedMessageFormatWithTypeDetails(MatcherConfig.ErrorDescriptionType errorDescriptionType,
                                              Object isEqualValue,
                                              Object jsonValue,
                                              boolean expectedSuccess,
                                              String expectedMessage) {
        final RestAssuredConfig config = RestAssuredConfig.newConfig()
                .matcherConfig(MatcherConfig.matcherConfig()
                        .errorDescriptionType(errorDescriptionType));

        final BodyMatcher bodyMatcher = new BodyMatcher();
        bodyMatcher.setKey("foo");
        bodyMatcher.setMatcher(new IsEqual<>(isEqualValue));
        bodyMatcher.setRpr(responseParserRegistrar);

        final Response response = new ResponseBuilder()
                .setStatusCode(200)
                .setBody("{\"foo\": " + jsonValue + "}")
                .setContentType(ContentType.JSON)
                .build();

        final Map<String, Object> result = bodyMatcher.validate(response,
                new ContentParser().parse(response, responseParserRegistrar, config, true),
                config);

        assertThat(result.get("success"), equalTo(expectedSuccess));
        assertThat(result.get("errorMessage"), equalTo(expectedMessage));
    }
}