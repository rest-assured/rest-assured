package io.restassured.itest.java;

import com.google.gson.reflect.TypeToken;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.itest.java.objects.Message;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static io.restassured.mapper.ObjectMapperType.JACKSON_1;
import static io.restassured.mapper.ObjectMapperType.JACKSON_2;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class TypeObjectMappingITest extends WithJetty {

    @Parameterized.Parameter
    public ObjectMapperType mapperType;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {GSON},
                {JACKSON_1},
                {JACKSON_2}
        });
    }

    @Test
    public void shouldUseMapTypeWithObjectMappers() {
        String expected = "A message";
        final Message message = new Message();
        message.setMessage(expected);
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig(mapperType));
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        final Map<String, String> returnedMessage = given().body(message).when().post("/reflect").as(type);
        assertThat(returnedMessage.get("message"), equalTo(expected));
    }
}
