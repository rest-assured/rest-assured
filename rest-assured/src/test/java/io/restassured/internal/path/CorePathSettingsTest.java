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
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CorePathSettingsTest {

    private static final RestAssuredConfig CONFIG = RestAssuredConfig.config()
            .jsonConfig(JsonConfig.jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL).numberLengthLimit(5))
            .xmlConfig(XmlConfig.xmlConfig().feature("urn:feature", true).property("urn:property", "v").declareNamespace("a", "urn:a")
                    .validating(true).namespaceAware(false).allowDocTypeDeclaration(true));

    @Test
    void body_assertions_use_all_of_the_json_and_xml_config() {
        assertThat(CorePathSettings.forBodyAssertions(CONFIG)).isEqualTo(new PathSettings(NumberReturnType.BIG_DECIMAL, 5, true, false, true,
                Collections.singletonMap("urn:feature", true), Collections.singletonMap("urn:property", "v"), Collections.singletonMap("a", "urn:a")));
    }

    @Test
    void path_method_keeps_the_defaults_of_json_path_and_xml_path_for_the_length_limit_and_the_parser_flags() {
        assertThat(CorePathSettings.forPathMethod(CONFIG)).isEqualTo(new PathSettings(NumberReturnType.BIG_DECIMAL, 1000, false, true, false,
                Collections.singletonMap("urn:feature", true), Collections.singletonMap("urn:property", "v"), Collections.singletonMap("a", "urn:a")));
    }
}
