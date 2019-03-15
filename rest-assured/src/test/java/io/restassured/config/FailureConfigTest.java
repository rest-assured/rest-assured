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

package io.restassured.config;

import io.restassured.listener.ResponseValidationFailureListener;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class FailureConfigTest {

    @Test
    public void
    shouldNotOverwriteFailureConfig() {
        // Given
        FailureConfig failureConfig = new FailureConfig().failureListeners(new ResponseValidationFailureListener() {
            @Override
            public void onFailure(RequestSpecification requestSpecification, ResponseSpecification responseSpecification, Response response) {
            }
        });
        // When
        RestAssuredConfig config = RestAssuredConfig.config().failureConfig(failureConfig)
                .httpClient(new HttpClientConfig())
                .redirect(new RedirectConfig())
                .logConfig(new LogConfig())
                .encoderConfig(new EncoderConfig())
                .decoderConfig(new DecoderConfig())
                .sessionConfig(new SessionConfig())
                .objectMapperConfig(new ObjectMapperConfig())
                .connectionConfig(new ConnectionConfig())
                .jsonConfig(new JsonConfig())
                .xmlConfig(new XmlConfig())
                .sslConfig(new SSLConfig())
                .matcherConfig(new MatcherConfig())
                .headerConfig(new HeaderConfig())
                .multiPartConfig(new MultiPartConfig())
                .paramConfig(new ParamConfig())
                .oauthConfig(new OAuthConfig());
        // Then
        assertThat(config.getFailureConfig().getFailureListeners(), hasSize(2));
    }
}