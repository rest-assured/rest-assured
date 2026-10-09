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

public class OAuthScheme implements AuthenticationScheme {

    private String consumerKey;
    private String consumerSecret;
    private String accessToken;
    private String secretToken;
    private OAuthSignature signature;

    @Override
    public void authenticate(HTTPBuilder httpBuilder) {
        if (signature != null) {
            httpBuilder.getAuth().oauth(consumerKey, consumerSecret, accessToken, secretToken, signature);
        } else {
            httpBuilder.getAuth().oauth(consumerKey, consumerSecret, accessToken, secretToken);
        }
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getSecretToken() {
        return secretToken;
    }

    public void setSecretToken(String secretToken) {
        this.secretToken = secretToken;
    }

    public OAuthSignature getSignature() {
        return signature;
    }

    public void setSignature(OAuthSignature signature) {
        this.signature = signature;
    }
}
