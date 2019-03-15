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
package io.restassured.internal.mapping

import io.restassured.internal.path.xml.mapping.XmlPathJaxbObjectDeserializer
import io.restassured.mapper.ObjectMapper
import io.restassured.mapper.ObjectMapperDeserializationContext
import io.restassured.mapper.ObjectMapperSerializationContext
import io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory
import io.restassured.path.xml.mapping.XmlPathObjectDeserializer

import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

class JaxbMapper implements ObjectMapper {

    private final JAXBObjectMapperFactory factory;

    private XmlPathObjectDeserializer deserializer

    public JaxbMapper(JAXBObjectMapperFactory factory) {
        this.factory = factory
        deserializer = new XmlPathJaxbObjectDeserializer(factory)
    }

    def Object deserialize(ObjectMapperDeserializationContext context) {
        deserializer.deserialize(context)
    }

    def Object serialize(ObjectMapperSerializationContext context) {
        def object = context.getObjectToSerialize();
        def charset = context.getCharset()
        JAXBContext jaxbContext = factory.create(object.getClass(), charset)
        Marshaller marshaller = jaxbContext.createMarshaller()
        if (charset != null) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, charset)
        }
        StringWriter sw = new StringWriter()
        marshaller.marshal(object, sw)
        return sw.toString()
    }
}
