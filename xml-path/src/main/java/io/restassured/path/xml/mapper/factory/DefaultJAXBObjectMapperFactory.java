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

package io.restassured.path.xml.mapper.factory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.lang.reflect.Type;

/**
 * Simply creates a new JAXBContext based on the supplied class.
 */
public class DefaultJAXBObjectMapperFactory implements JAXBObjectMapperFactory {
    public JAXBContext create(Type cls, String charset) {
        try {
            if (cls instanceof Class) {
                return JAXBContext.newInstance((Class<?>) cls);
            }
            throw new RuntimeException("JAXB does not support type" + cls);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}