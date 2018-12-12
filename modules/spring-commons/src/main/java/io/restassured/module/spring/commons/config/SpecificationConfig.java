/*
 * Copyright 2016-2018 the original author or authors.
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

import io.restassured.config.DecoderConfig;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HeaderConfig;
import io.restassured.config.JsonConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.MultiPartConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.ParamConfig;
import io.restassured.config.SessionConfig;
import io.restassured.config.XmlConfig;

public interface SpecificationConfig {

    boolean isUserConfigured();

    DecoderConfig getDecoderConfig();

    SpecificationConfig decoderConfig(DecoderConfig decoderConfig);

    EncoderConfig getEncoderConfig();

    SpecificationConfig encoderConfig(EncoderConfig encoderConfig);

    HeaderConfig getHeaderConfig();

    SpecificationConfig headerConfig(HeaderConfig headerConfig);

    JsonConfig getJsonConfig();

    SpecificationConfig jsonConfig(JsonConfig jsonConfig);

    LogConfig getLogConfig();

    SpecificationConfig logConfig(LogConfig logConfig);

    ObjectMapperConfig getObjectMapperConfig();

    SpecificationConfig objectMapperConfig(ObjectMapperConfig objectMapperConfig);

    SessionConfig getSessionConfig();

    SpecificationConfig sessionConfig(SessionConfig sessionConfig);

    XmlConfig getXmlConfig();

    SpecificationConfig xmlConfig(XmlConfig xmlConfig);

    AsyncConfig getAsyncConfig();

    SpecificationConfig asyncConfig(AsyncConfig asyncConfig);

    MultiPartConfig getMultiPartConfig();

    SpecificationConfig multiPartConfig(MultiPartConfig multiPartConfig);

    ClientConfig getClientConfig();

    SpecificationConfig clientConfig(ClientConfig clientConfig);

    ParamConfig getParamConfig();

    SpecificationConfig paramConfig(ParamConfig paramConfig);

}
