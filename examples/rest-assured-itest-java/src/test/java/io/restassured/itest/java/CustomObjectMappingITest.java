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
package io.restassured.itest.java;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.itest.java.objects.Greeting;
import io.restassured.itest.java.objects.Message;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.path.json.mapper.factory.GsonObjectMapperFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
import static io.restassured.RestAssured.given;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CustomObjectMappingITest extends WithJetty {
    public AtomicBoolean customSerializationUsed = new AtomicBoolean(false);
    public AtomicBoolean customDeserializationUsed = new AtomicBoolean(false);

    @Before public void
    setup() throws Exception {
        customSerializationUsed.set(false);
        customDeserializationUsed.set(false);
    }

    @Test public void
    using_explicit_custom_object_mapper() throws Exception {
        final Message message = new Message();
        message.setMessage("A message");
        final ObjectMapper mapper = new ObjectMapper() {
            public Object deserialize(ObjectMapperDeserializationContext context) {
                final String toDeserialize = context.getDataToDeserialize().asString();
                final String unquoted = StringUtils.remove(toDeserialize, "#");
                final Message message = new Message();
                message.setMessage(unquoted);
                customDeserializationUsed.set(true);
                return message;
            }

            public Object serialize(ObjectMapperSerializationContext context) {
                final Message objectToSerialize = context.getObjectToSerializeAs(Message.class);
                final String message = objectToSerialize.getMessage();
                customSerializationUsed.set(true);
                return "##" + message + "##";
            }
        };

        final Message returnedMessage = given().body(message, mapper).when().post("/reflect").as(Message.class, mapper);

        assertThat(returnedMessage.getMessage(), equalTo("A message"));
        assertThat(customSerializationUsed.get(), is(true));
        assertThat(customDeserializationUsed.get(), is(true));
    }

    @Test public void
    using_custom_object_mapper_statically() {
        final Message message = new Message();
        message.setMessage("A message");
        final ObjectMapper mapper = new ObjectMapper() {
            public Object deserialize(ObjectMapperDeserializationContext context) {
                final String toDeserialize = context.getDataToDeserialize().asString();
                final String unquoted = StringUtils.remove(toDeserialize, "##");
                final Message message = new Message();
                message.setMessage(unquoted);
                customDeserializationUsed.set(true);
                return message;
            }

            public Object serialize(ObjectMapperSerializationContext context) {
                final Message objectToSerialize = context.getObjectToSerializeAs(Message.class);
                final String message = objectToSerialize.getMessage();
                customSerializationUsed.set(true);
                return "##" + message + "##";
            }
        };
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig(mapper));

        final Message returnedMessage = given().body(message).when().post("/reflect").as(Message.class);

        assertThat(returnedMessage.getMessage(), equalTo("A message"));
        assertThat(customSerializationUsed.get(), is(true));
        assertThat(customDeserializationUsed.get(), is(true));
    }

    @Test public void
    using_default_object_mapper_type_if_specified() {
        final Message message = new Message();
        message.setMessage("A message");
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig(GSON));

        final Message returnedMessage = given().body(message).when().post("/reflect").as(Message.class);

        assertThat(returnedMessage.getMessage(), equalTo("A message"));
    }

    @Test
    public void
    using_as_specified_object() {
        final Message message = new Message();
        message.setMessage("A message");
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig(GSON));

        final String returnedMessage = given().body(message).when().post("/reflect")
                .as(Message.class).getMessage();

        assertThat(returnedMessage, equalTo("A message"));
    }

    @Test public void
    using_custom_object_mapper_factory() {
        final Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(objectMapperConfig().gsonObjectMapperFactory(
                new GsonObjectMapperFactory() {
                    public Gson create(Type cls, String charset) {
                        return new GsonBuilder().setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES).create();
                    }
                }
        ));

        final Greeting returnedGreeting = given().contentType("application/json").body(greeting, GSON).
                expect().body("first_name", equalTo("John")).when().post("/reflect").as(Greeting.class, GSON);

        assertThat(returnedGreeting.getFirstName(), equalTo("John"));
        assertThat(returnedGreeting.getLastName(), equalTo("Doe"));
    }
}
