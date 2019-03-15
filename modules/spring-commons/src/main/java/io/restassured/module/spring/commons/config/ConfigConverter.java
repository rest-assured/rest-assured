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

package io.restassured.module.spring.commons.config;

import io.restassured.config.ParamConfig;
import io.restassured.config.RestAssuredConfig;

import java.lang.reflect.Field;

public class ConfigConverter {

    public static RestAssuredConfig convertToRestAssuredConfig(SpecificationConfig specificationConfig) {
        return new RestAssuredConfig().jsonConfig(specificationConfig.getJsonConfig()).xmlConfig(specificationConfig.getXmlConfig()).sessionConfig(specificationConfig.getSessionConfig()).
                objectMapperConfig(specificationConfig.getObjectMapperConfig()).logConfig(specificationConfig.getLogConfig()).encoderConfig(specificationConfig.getEncoderConfig()).
                decoderConfig(specificationConfig.getDecoderConfig()).multiPartConfig(specificationConfig.getMultiPartConfig()).paramConfig(toParamConfig(specificationConfig.getParamConfig())).
                matcherConfig(specificationConfig.getMatcherConfig());
    }

    private static ParamConfig toParamConfig(ParamConfig baseConfig) {
        ParamConfig config = new ParamConfig(baseConfig.queryParamsUpdateStrategy(),
                baseConfig.formParamsUpdateStrategy(), baseConfig.requestParamsUpdateStrategy());
        // We need to set the user configured flag to false if needed
        if (!baseConfig.isUserConfigured()) {
            Field userConfigured = null;
            try {
                userConfigured = config.getClass().getDeclaredField("userConfigured");
                userConfigured.setAccessible(true);
                userConfigured.set(config, false);
            } catch (Exception e) {
                throw new RuntimeException("Internal error in REST Assured, please report an issue!", e);
            } finally {
                if (userConfigured != null) {
                    userConfigured.setAccessible(false);
                }
            }
        }
        return config;
    }
}
