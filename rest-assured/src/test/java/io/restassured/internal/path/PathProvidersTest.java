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

import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.common.spi.path.PathResult;
import io.restassured.internal.path.json.GPathJsonProvider;
import io.restassured.internal.path.xml.GPathXmlProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathProvidersTest {

    @Test
    void finds_gpath_for_each_document_type() {
        RestAssuredConfig config = RestAssuredConfig.config();

        assertThat(PathProviders.forType(DocumentType.JSON, config)).isExactlyInstanceOf(GPathJsonProvider.class);
        assertThat(PathProviders.forType(DocumentType.XML, config)).isExactlyInstanceOf(GPathXmlProvider.class);
        assertThat(PathProviders.forType(DocumentType.HTML, config)).isExactlyInstanceOf(GPathXmlProvider.class);
    }

    @Test
    void loads_each_provider_once() {
        assertThat(PathProviders.load(PathProviders.GPATH_JSON)).containsSame(PathProviders.load(PathProviders.GPATH_JSON).orElseThrow());
    }

    @Test
    void a_provider_that_isnt_on_the_classpath_is_absent() {
        assertThat(PathProviders.load("io.restassured.path.missing.Provider")).isEmpty();
    }

    @Test
    void a_missing_path_is_null() {
        assertThat(PathProviders.valueOf(PathResult.value("x"))).isEqualTo("x");
        assertThat(PathProviders.valueOf(PathResult.value(null))).isNull();
        assertThat(PathProviders.valueOf(PathResult.missing())).isNull();
    }

    @Test
    void a_list_addressed_as_an_object_fails_with_the_message_of_the_engine() {
        assertThatThrownBy(() -> PathProviders.valueOf(PathResult.listAddressedAsObject("a is a list, use a[*].b")))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("a is a list, use a[*].b");
    }
}
