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
package io.restassured.config;

import org.junit.jupiter.api.Test;

import static io.restassured.config.CsrfConfig.csrfConfig;
import static org.assertj.core.api.Assertions.assertThat;

class CsrfConfigTest {

    @Test
    void csrf_meta_tag_name_keeps_the_configured_input_field_name() {
        CsrfConfig config = csrfConfig().csrfInputFieldName("my_field").csrfMetaTagName("my_meta");

        assertThat(config.getCsrfInputFieldName()).isEqualTo("my_field");
        assertThat(config.getCsrfMetaTagName()).isEqualTo("my_meta");
    }

    @Test
    void csrf_meta_tag_name_keeps_the_default_input_field_name() {
        CsrfConfig config = csrfConfig().csrfMetaTagName("my_meta");

        assertThat(config.getCsrfInputFieldName()).isEqualTo(CsrfConfig.DEFAULT_CSRF_INPUT_FIELD_NAME);
        assertThat(config.getCsrfMetaTagName()).isEqualTo("my_meta");
    }
}
