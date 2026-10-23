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

package io.restassured.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeMovedTypesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        RecipeTestSupport.defaults(spec);
        spec.recipeFromResources("io.restassured.ChangeObjectMapperFactoryPackage", "io.restassured.ChangeNumberReturnTypePackage");
    }

    @Test
    void changes_object_mapper_factories_given_to_configs_and_paths() {
        rewriteRun(
                java(
                        """
                        import io.restassured.config.ObjectMapperConfig;
                        import io.restassured.path.json.JsonPath;
                        import io.restassured.path.json.config.JsonPathConfig;
                        import io.restassured.path.json.mapper.factory.DefaultJackson2ObjectMapperFactory;
                        import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory;
                        import io.restassured.path.xml.XmlPath;
                        import io.restassured.path.xml.config.XmlPathConfig;
                        import io.restassured.path.xml.mapper.factory.DefaultJAXBObjectMapperFactory;
                        import io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory;

                        class A {
                            Jackson2ObjectMapperFactory jackson = new DefaultJackson2ObjectMapperFactory();
                            JAXBObjectMapperFactory jaxb = new DefaultJAXBObjectMapperFactory();
                            ObjectMapperConfig config = ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory(jackson).jaxbObjectMapperFactory(jaxb);
                            JsonPathConfig jsonPathConfig = JsonPathConfig.jsonPathConfig().jackson2ObjectMapperFactory(jackson);
                            XmlPathConfig xmlPathConfig = XmlPathConfig.xmlPathConfig().jaxbObjectMapperFactory(jaxb);
                            JsonPath jsonPath = JsonPath.from("{}").using(jackson);
                            XmlPath xmlPath = XmlPath.from("<a/>").using(jaxb);
                            Jackson2ObjectMapperFactory configured = config.jackson2ObjectMapperFactory();
                        }
                        """,
                        """
                        import io.restassured.common.mapper.factory.DefaultJAXBObjectMapperFactory;
                        import io.restassured.common.mapper.factory.DefaultJackson2ObjectMapperFactory;
                        import io.restassured.common.mapper.factory.JAXBObjectMapperFactory;
                        import io.restassured.common.mapper.factory.Jackson2ObjectMapperFactory;
                        import io.restassured.config.ObjectMapperConfig;
                        import io.restassured.path.json.JsonPath;
                        import io.restassured.path.json.config.JsonPathConfig;
                        import io.restassured.path.xml.XmlPath;
                        import io.restassured.path.xml.config.XmlPathConfig;

                        class A {
                            Jackson2ObjectMapperFactory jackson = new DefaultJackson2ObjectMapperFactory();
                            JAXBObjectMapperFactory jaxb = new DefaultJAXBObjectMapperFactory();
                            ObjectMapperConfig config = ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory(jackson).jaxbObjectMapperFactory(jaxb);
                            JsonPathConfig jsonPathConfig = JsonPathConfig.jsonPathConfig().jackson2ObjectMapperFactory(jackson);
                            XmlPathConfig xmlPathConfig = XmlPathConfig.xmlPathConfig().jaxbObjectMapperFactory(jaxb);
                            JsonPath jsonPath = JsonPath.from("{}").using(jackson);
                            XmlPath xmlPath = XmlPath.from("<a/>").using(jaxb);
                            Jackson2ObjectMapperFactory configured = config.jackson2ObjectMapperFactory();
                        }
                        """
                )
        );
    }

    @Test
    void changes_implemented_and_fully_qualified_object_mapper_factories_and_imports_them() {
        rewriteRun(
                java(
                        """
                        import com.fasterxml.jackson.databind.ObjectMapper;
                        import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory;

                        import java.lang.reflect.Type;

                        class MyFactory implements Jackson2ObjectMapperFactory {
                            @Override
                            public ObjectMapper create(Type type, String charset) {
                                return new ObjectMapper();
                            }

                            io.restassured.path.json.mapper.factory.DefaultGsonObjectMapperFactory gson;
                        }
                        """,
                        """
                        import com.fasterxml.jackson.databind.ObjectMapper;
                        import io.restassured.common.mapper.factory.DefaultGsonObjectMapperFactory;
                        import io.restassured.common.mapper.factory.Jackson2ObjectMapperFactory;

                        import java.lang.reflect.Type;

                        class MyFactory implements Jackson2ObjectMapperFactory {
                            @Override
                            public ObjectMapper create(Type type, String charset) {
                                return new ObjectMapper();
                            }

                            DefaultGsonObjectMapperFactory gson;
                        }
                        """
                )
        );
    }

    @Test
    void changes_number_return_type() {
        rewriteRun(
                RecipeTestSupport::allowingRemovedJsonConfigMethods,
                java(
                        """
                        import io.restassured.config.JsonConfig;
                        import io.restassured.path.json.JsonPath;
                        import io.restassured.path.json.config.JsonPathConfig;

                        class A {
                            JsonPathConfig config = new JsonPathConfig(JsonPathConfig.NumberReturnType.BIG_DECIMAL);
                            JsonConfig jsonConfig = JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE);
                            JsonConfig otherJsonConfig = new JsonConfig(JsonPathConfig.NumberReturnType.BIG_INTEGER);
                            JsonPath path = JsonPath.from("{}").using(config.numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE));
                        }
                        """,
                        """
                        import io.restassured.common.config.NumberReturnType;
                        import io.restassured.config.JsonConfig;
                        import io.restassured.path.json.JsonPath;
                        import io.restassured.path.json.config.JsonPathConfig;

                        class A {
                            JsonPathConfig config = new JsonPathConfig(NumberReturnType.BIG_DECIMAL);
                            JsonConfig jsonConfig = JsonConfig.jsonConfig().numberReturnType(NumberReturnType.DOUBLE);
                            JsonConfig otherJsonConfig = new JsonConfig(NumberReturnType.BIG_INTEGER);
                            JsonPath path = JsonPath.from("{}").using(config.numberReturnType(NumberReturnType.FLOAT_AND_DOUBLE));
                        }
                        """
                )
        );
    }

    @Test
    void changes_imported_and_statically_imported_number_return_type() {
        rewriteRun(
                java(
                        """
                        import io.restassured.path.json.config.JsonPathConfig;
                        import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;

                        import static io.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;

                        class A {
                            NumberReturnType type = BIG_DECIMAL;
                            JsonPathConfig config = JsonPathConfig.jsonPathConfig().numberReturnType(BIG_DECIMAL).numberReturnType(NumberReturnType.DOUBLE);
                        }
                        """,
                        """
                        import io.restassured.common.config.NumberReturnType;
                        import io.restassured.path.json.config.JsonPathConfig;

                        import static io.restassured.common.config.NumberReturnType.BIG_DECIMAL;

                        class A {
                            NumberReturnType type = BIG_DECIMAL;
                            JsonPathConfig config = JsonPathConfig.jsonPathConfig().numberReturnType(BIG_DECIMAL).numberReturnType(NumberReturnType.DOUBLE);
                        }
                        """
                )
        );
    }
}
