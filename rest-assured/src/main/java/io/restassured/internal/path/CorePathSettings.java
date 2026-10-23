/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.internal.path;

import io.restassured.common.config.NumberReturnType;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.XmlConfig;
import io.restassured.internal.common.spi.path.PathSettings;

/**
 * The settings that body assertions and {@code path(..)} parse a response body with. They differ, as they always have:
 * {@code path(..)} uses what {@code jsonPath()} and {@code xmlPath()} pass to JsonPath and XmlPath.
 */
public final class CorePathSettings {

    private CorePathSettings() {
    }

    /**
     * @return All of the JSON and XML config.
     */
    public static PathSettings forBodyAssertions(RestAssuredConfig config) {
        JsonConfig jsonConfig = config.getJsonConfig();
        XmlConfig xmlConfig = config.getXmlConfig();
        return new PathSettings(numberReturnType(jsonConfig), jsonConfig.numberLengthLimit(), xmlConfig.isValidating(),
                xmlConfig.isNamespaceAware(), xmlConfig.isAllowDocTypeDeclaration(), xmlConfig.features(), xmlConfig.properties(),
                xmlConfig.declaredNamespaces());
    }

    /**
     * @return The number type of the JSON config, and the features, properties and namespaces of the XML config. The
     * number length limit and the XML parser flags are the defaults of JsonPathConfig and XmlPathConfig.
     */
    public static PathSettings forPathMethod(RestAssuredConfig config) {
        XmlConfig xmlConfig = config.getXmlConfig();
        return new PathSettings(numberReturnType(config.getJsonConfig()), JsonConfig.DEFAULT_NUMBER_LENGTH_LIMIT, false, true, false,
                xmlConfig.features(), xmlConfig.properties(), xmlConfig.declaredNamespaces());
    }

    private static NumberReturnType numberReturnType(JsonConfig jsonConfig) {
        return NumberReturnType.valueOf(jsonConfig.numberReturnType().name());
    }
}
