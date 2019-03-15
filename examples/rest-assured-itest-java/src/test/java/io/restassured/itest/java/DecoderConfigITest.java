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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import io.restassured.builder.ResponseBuilder;
import io.restassured.config.DecoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

import static io.restassured.RestAssured.*;
import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DecoderConfigITest extends WithJetty {

    @Test public void
    allows_specifying_content_decoders_statically() {
        // Given
        RestAssured.config = RestAssuredConfig.newConfig().decoderConfig(DecoderConfig.decoderConfig().contentDecoders(DecoderConfig.ContentDecoder.GZIP));
        try {
            expect().
                    body("Accept-Encoding", hasItem("gzip")).
                    body("Accept-Encoding", hasSize(1)).
            when().
                    get("/headersWithValues");
        } finally {
            RestAssured.reset();
        }
    }

    @Test public void
    allows_specifying_content_decoders_per_request() {
        given().
                config(RestAssuredConfig.newConfig().decoderConfig(DecoderConfig.decoderConfig().contentDecoders(DecoderConfig.ContentDecoder.GZIP))).
        expect().
                body("Accept-Encoding", contains("gzip")).
        when().
                get("/headersWithValues");
    }

    @Test public void
    uses_gzip_and_deflate_content_decoders_by_default() {
        expect().
                body("Accept-Encoding", contains("gzip,deflate")).
        when().
                get("/headersWithValues");
    }

    @Test public void
    decoder_config_can_be_configured_to_use_no_decoders() {
        String hasAcceptEncodingParameter = "$.any { it.containsKey('Accept-Encoding') }";

        given().
                config(RestAssuredConfig.newConfig().decoderConfig(DecoderConfig.decoderConfig().with().noContentDecoders())).
        expect().
                body(hasAcceptEncodingParameter, is(false)).
        when().
                get("/headersWithValues");
    }

    @Test public void
    filters_can_change_content_decoders() {
        given().
                config(RestAssuredConfig.newConfig().decoderConfig(DecoderConfig.decoderConfig().contentDecoders(DecoderConfig.ContentDecoder.GZIP, DecoderConfig.ContentDecoder.DEFLATE))).
                filter((requestSpec, responseSpec, ctx) -> {
                    final RestAssuredConfig config1 = requestSpec.getConfig();
                    assertThat(config1.getDecoderConfig().contentDecoders(), Matchers.contains(DecoderConfig.ContentDecoder.GZIP, DecoderConfig.ContentDecoder.DEFLATE));
                    requestSpec.config(config1.decoderConfig(new DecoderConfig(DecoderConfig.ContentDecoder.DEFLATE)));
                    return ctx.next(requestSpec, responseSpec);
                }).
        expect().
                body("Accept-Encoding", contains("deflate")).
        when().
                get("/headersWithValues");
    }

    @Test @Ignore("External service")  public void
    use_no_wrap_for_inflated_streams_supports_gzdeflate_method() {
        RestAssured.baseURI = "https://charitablegift.fidelity.com";

        RestAssured.port = 443;
        RestAssured.basePath = "/externalApps/public";

        String path = "/mobile";

        Response response = expect().statusCode(200).given().relaxedHTTPSValidation()
                .params("Oper_Name", "getConfigInfo",
                        "Config_Ver", "1.0",
                        "Os_Ver", "5.0.1",
                        "Device_Type", "iPhone OS",
                        "App_Ver", "1.0",
                        "uApp_Id", "MDNR")
               .config(config().decoderConfig(DecoderConfig.decoderConfig().useNoWrapForInflateDecoding(true))).when().get(path);

        assertThat(response.getBody().asString(), not(emptyOrNullString()));
    }

    @Test public void
    json_content_type_is_configured_to_use_utf_8_by_default() {
        given().
                config(RestAssuredConfig.newConfig().decoderConfig(DecoderConfig.decoderConfig().defaultContentCharset(UTF_16))).
        when().
                get("/lotto").
        then().
                header("Content-Type", equalTo("application/json;charset=" + DecoderConfig.decoderConfig().defaultCharsetForContentType(ContentType.JSON).toLowerCase()));
    }

    @Test public void
    can_specify_default_charset_for_a_specific_content_type() {
        given().
                config(RestAssuredConfig.newConfig().decoderConfig(DecoderConfig.decoderConfig().defaultCharsetForContentType(UTF_16, ContentType.TEXT))).
                filter((requestSpec, responseSpec, ctx) -> new ResponseBuilder().setStatusCode(200).setBody("something\ud824\udd22").setContentType("text/plain").build()).
        when().
                get("/non-existing").
        then().
                body(equalTo("something\uD824\uDD22"));
    }
}
