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

package io.restassured.internal.path.xml.mapping;

import io.restassured.common.mapper.factory.JAXBObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultJAXBObjectMapperFactory;
import io.restassured.common.mapper.factory.JakartaEEObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultJakartaEEObjectMapperFactory;

/**
 * Turns the object mapper factory types in rest-assured-common into the deprecated ones that the configs still return in 6.x. A default factory becomes the matching deprecated default, so that it doesn't count as a custom factory.
 */
@SuppressWarnings("deprecation")
public class LegacyXmlObjectMapperFactories {

    private LegacyXmlObjectMapperFactories() {
    }

    public static io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory toLegacy(JAXBObjectMapperFactory factory) {
        if (factory == null || factory instanceof io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory) {
            return (io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory) factory;
        } else if (factory.getClass() == DefaultJAXBObjectMapperFactory.class) {
            return new io.restassured.path.xml.mapper.factory.DefaultJAXBObjectMapperFactory();
        }
        return factory::create;
    }

    public static io.restassured.path.xml.mapper.factory.JakartaEEObjectMapperFactory toLegacy(JakartaEEObjectMapperFactory factory) {
        if (factory == null || factory instanceof io.restassured.path.xml.mapper.factory.JakartaEEObjectMapperFactory) {
            return (io.restassured.path.xml.mapper.factory.JakartaEEObjectMapperFactory) factory;
        } else if (factory.getClass() == DefaultJakartaEEObjectMapperFactory.class) {
            return new io.restassured.path.xml.mapper.factory.DefaultJakartaEEObjectMapperFactory();
        }
        return factory::create;
    }
}
