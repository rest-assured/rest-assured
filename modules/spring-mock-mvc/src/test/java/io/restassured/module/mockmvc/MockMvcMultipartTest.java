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

package io.restassured.module.mockmvc;

import io.restassured.module.mockmvc.http.MultipartController;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MockMvcMultipartTest {

    @Test
    public void can_use_multipart_with_path_params() {
        RestAssuredMockMvc.given()
                .standaloneSetup(new MultipartController())
                .multiPart("file", "Test")
                .when()
                .post("/files/{type}", (Object[]) "type".split("\\|"))
                .then()
                .log().all()
                .time(lessThan(3L), SECONDS)
                .expect(status().is2xxSuccessful())
                .body("type", equalTo("type"))
                .body("name", equalTo("file"));
    }

}
