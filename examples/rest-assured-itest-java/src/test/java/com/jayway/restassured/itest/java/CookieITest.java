/*
 * Copyright 2011 the original author or authors.
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
import com.jayway.restassured.response.Cookies;
import org.junit.Test;

import java.util.Map;

import static com.jayway.restassured.RestAssured.get;

public class CookieITest extends WithJetty {

    @Test
    public void test() throws Exception {
        final Map<String,String> cookies = get("/springCookie").cookies();

        System.out.println(cookies);
    }

    @Test
    public void test2() throws Exception {
        final Cookies cookies = get("/springCookie").detailedCookies();

        System.out.println(cookies);
    }
}
