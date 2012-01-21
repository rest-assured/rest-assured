/*
 * Copyright 2012 the original author or authors.
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

import com.jayway.restassured.itest.java.support.WithJetty;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class ParamITest extends WithJetty {

    @Test
    public void noValueParamWhenUsingParamWithGetRequest() throws Exception {
        given().param("some").expect().body(is("OK")).when().get("/noValueParam");
    }

    @Test
    public void noValueParamWhenUsingQueryParamWithGetRequest() throws Exception {
        given().queryParam("some").expect().body(is("OK")).when().get("/noValueParam");
    }

    @Test
    public void noValueParamWhenUsingFormParamWithPutRequest() throws Exception {
        given().formParam("some").expect().body(is("OK")).when().put("/noValueParam");
    }

    @Test
    public void noValueParamWhenUsingFormParamWithPostRequest() throws Exception {
        given().formParam("some").expect().body(is("OK")).when().post("/noValueParam");
    }

    @Test
    public void noValueParamWhenUsingParamWithPostRequest() throws Exception {
        given().param("some").expect().body(is("OK")).when().post("/noValueParam");
    }

    @Test
    public void multiPartUploadingWorksForFormParamsAndByteArray() throws Exception {
        given().
                formParam("formParam1").
                formParam("formParam2", "formParamValue").
                multiPart("file", "juX").
                multiPart("string", "body").
        expect().
                statusCode(200).
                body(containsString("formParam1 -> WrappedArray()")).
        when().
                post("/multipart/multiple");
    }
}
