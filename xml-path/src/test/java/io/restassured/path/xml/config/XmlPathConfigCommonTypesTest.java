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

package io.restassured.path.xml.config;

import io.restassured.common.mapper.factory.DefaultJAXBObjectMapperFactory;
import io.restassured.common.mapper.factory.JAXBObjectMapperFactory;
import org.junit.jupiter.api.Test;

import static io.restassured.path.xml.config.XmlPathConfig.xmlPathConfig;
import static org.assertj.core.api.Assertions.assertThat;

class XmlPathConfigCommonTypesTest {

    @Test
    void common_default_jaxb_factory_doesnt_count_as_custom() {
        assertThat(xmlPathConfig().jaxbObjectMapperFactory(new DefaultJAXBObjectMapperFactory()).hasCustomJaxbObjectMapperFactory()).isFalse();
    }

    @Test
    void custom_jaxb_factory_of_the_common_type_is_returned_as_the_old_type() {
        JAXBObjectMapperFactory factory = (type, charset) -> null;

        XmlPathConfig config = xmlPathConfig().jaxbObjectMapperFactory(factory);

        assertThat(config.hasCustomJaxbObjectMapperFactory()).isTrue();
        assertThat(config.jaxbObjectMapperFactory().create(Object.class, "UTF-8")).isNull();
    }
}
