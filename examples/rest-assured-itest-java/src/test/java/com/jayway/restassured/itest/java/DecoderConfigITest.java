/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.DecoderConfig;
import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.DecoderConfig.ContentDecoder.DEFLATE;
import static com.jayway.restassured.config.DecoderConfig.ContentDecoder.GZIP;
import static com.jayway.restassured.config.DecoderConfig.decoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DecoderConfigITest extends WithJetty {

    @Test public void
    allows_specifying_content_decoders_statically() {
        // Given
        RestAssured.config = newConfig().decoderConfig(decoderConfig().contentDecoders(GZIP));
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
                config(newConfig().decoderConfig(decoderConfig().contentDecoders(GZIP))).
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
                config(newConfig().decoderConfig(decoderConfig().with().noContentDecoders())).
        expect().
                body(hasAcceptEncodingParameter, is(false)).
        when().
                get("/headersWithValues");
    }

    @Test public void
    filters_can_change_content_decoders() {
        given().
                config(newConfig().decoderConfig(decoderConfig().contentDecoders(GZIP, DEFLATE))).
                filter(new Filter() {
                    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
                        final RestAssuredConfig config = requestSpec.getConfig();
                        assertThat(config.getDecoderConfig().contentDecoders(), contains(GZIP, DEFLATE));
                        requestSpec.config(config.decoderConfig(new DecoderConfig(DEFLATE)));
                        return ctx.next(requestSpec, responseSpec);
                    }
                }).
        expect().
                body("Accept-Encoding", contains("deflate")).
        when().
                get("/headersWithValues");
    }
}
