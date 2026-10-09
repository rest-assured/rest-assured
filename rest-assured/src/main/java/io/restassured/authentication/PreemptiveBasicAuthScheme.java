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

package io.restassured.authentication;

import io.restassured.internal.http.HTTPBuilder;

import java.util.Base64;
import java.util.Map;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * Used for basic and digest authentication
 */
public class PreemptiveBasicAuthScheme implements AuthenticationScheme {

    private String userName;
    private String password;

    @Override
    @SuppressWarnings("unchecked")
    public void authenticate(HTTPBuilder httpBuilder) {
        ((Map<Object, Object>) httpBuilder.getHeaders()).put("Authorization", generateAuthToken());
    }

    public String generateAuthToken() {
        return "Basic " + Base64.getEncoder().encodeToString((userName + ":" + password).getBytes(ISO_8859_1));
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
