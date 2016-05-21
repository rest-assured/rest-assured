/*
 * Copyright 2016 the original author or authors.
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

package io.restassured.module.mockmvc.internal;

import io.restassured.config.ParamConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.module.mockmvc.config.MockMvcParamConfig;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;

import java.lang.reflect.Field;

import static io.restassured.module.mockmvc.internal.UpdateStrategyConverter.convert;

class ConfigConverter {

    public static RestAssuredConfig convertToRestAssuredConfig(RestAssuredMockMvcConfig mvcConfig) {
        return new RestAssuredConfig().jsonConfig(mvcConfig.getJsonConfig()).xmlConfig(mvcConfig.getXmlConfig()).sessionConfig(mvcConfig.getSessionConfig()).
                objectMapperConfig(mvcConfig.getObjectMapperConfig()).logConfig(mvcConfig.getLogConfig()).encoderConfig(mvcConfig.getEncoderConfig()).
                decoderConfig(mvcConfig.getDecoderConfig()).multiPartConfig(mvcConfig.getMultiPartConfig()).paramConfig(toParamConfig(mvcConfig.getParamConfig()));
    }

    private static ParamConfig toParamConfig(MockMvcParamConfig cfg) {
        ParamConfig config = new ParamConfig(convert(cfg.queryParamsUpdateStrategy()),
                convert(cfg.formParamsUpdateStrategy()), convert(cfg.requestParamsUpdateStrategy()));
        // We need to set the user configured flag to false if needed
        if (!cfg.isUserConfigured()) {
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
