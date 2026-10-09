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

package io.restassured.internal.mapping;

import io.restassured.internal.path.xml.mapping.XmlPathJakartaEEObjectDeserializer;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.path.xml.mapper.factory.JakartaEEObjectMapperFactory;
import io.restassured.path.xml.mapping.XmlPathObjectDeserializer;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.io.StringWriter;

public class JakartaEEMapper implements ObjectMapper {

    private final JakartaEEObjectMapperFactory factory;

    private final XmlPathObjectDeserializer deserializer;

    public JakartaEEMapper(JakartaEEObjectMapperFactory factory) {
        this.factory = factory;
        deserializer = new XmlPathJakartaEEObjectDeserializer(factory);
    }

    public Object deserialize(ObjectMapperDeserializationContext context) {
        return deserializer.deserialize(context);
    }

    public Object serialize(ObjectMapperSerializationContext context) {
        Object object = context.getObjectToSerialize();
        String charset = context.getCharset();
        JAXBContext jaxbContext = factory.create(MapperSupport.typeOf(object), charset);
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            if (charset != null) {
                marshaller.setProperty(Marshaller.JAXB_ENCODING, charset);
            }
            StringWriter sw = new StringWriter();
            marshaller.marshal(object, sw);
            return sw.toString();
        } catch (JAXBException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }
}
