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
import static org.hamcrest.Matchers.is;

public class ParamITest extends WithJetty {

    @Test
    public void test() throws Exception {
        given().log().parameters().param("some").when().get("/mimeTypeWithPlusJson");
    }

    @Test
    public void test2() throws Exception {
        given().log().parameters().formParam("some").when().put("/mimeTypeWithPlusJson");
    }

    @Test
    public void test3() throws Exception {
        given().log().parameters().formParam("some").when().post("/mimeTypeWithPlusJson");
    }

    @Test
    public void multiPartUploadingWorksForFormParamsAndByteArray() throws Exception {
        // When
        given().
                formParam("formParam1").
                formParam("formParam2", "formParamValue").
                multiPart("file", "juX").
                multiPart("string", "body").
        expect().
                statusCode(200).
        when().
                post("/multipart/multiple");
    }
}
