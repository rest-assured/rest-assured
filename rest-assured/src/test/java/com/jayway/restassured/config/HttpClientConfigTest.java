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

package com.jayway.restassured.config;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

public class HttpClientConfigTest {

    @Test
    public void cookiePolicyIsSetToIgnoreCookiesByDefault() throws Exception {
        final HttpClientConfig httpClientConfig = new HttpClientConfig();

        assertThat((Map<String,String>) httpClientConfig.params(), hasEntry(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES));
    }
}
