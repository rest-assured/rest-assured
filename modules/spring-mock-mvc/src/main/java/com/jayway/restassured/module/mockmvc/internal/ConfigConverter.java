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

package com.jayway.restassured.module.mockmvc.internal;

import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.module.mockmvc.config.RestAssuredConfigMockMvc;

class ConfigConverter {

    public static RestAssuredConfig convertToRestAssuredConfig(RestAssuredConfigMockMvc mvcConfig) {
        return new RestAssuredConfig().jsonConfig(mvcConfig.getJsonConfig()).xmlConfig(mvcConfig.getXmlConfig()).sessionConfig(mvcConfig.getSessionConfig()).
                objectMapperConfig(mvcConfig.getObjectMapperConfig()).logConfig(mvcConfig.getLogConfig()).encoderConfig(mvcConfig.getEncoderConfig()).
                decoderConfig(mvcConfig.getDecoderConfig());
    }
}