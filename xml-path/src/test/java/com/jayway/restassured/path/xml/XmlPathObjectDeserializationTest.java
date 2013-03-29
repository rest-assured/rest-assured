package com.jayway.restassured.path.xml;

import com.jayway.restassured.path.xml.support.CoolGreeting;
import com.jayway.restassured.path.xml.support.Greeting;
import org.junit.Test;

import static com.jayway.restassured.path.xml.XmlPath.from;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class XmlPathObjectDeserializationTest {

    private static final String COOL_GREETING = "<cool><greeting><firstName>John</firstName>\n" +
            "      <lastName>Doe</lastName>\n" +
            "    </greeting></cool>";

    public static final String GREETINGS = "<greetings>\n" +
            "\t<greeting>\n" +
            "\t\t<firstName>John</firstName>\n" +
            "\t\t<lastName>Doe</lastName>\n" +
            "\t</greeting>\n" +
            "\t<greeting>\n" +
            "\t\t<firstName>Jane</firstName>\n" +
            "\t\t<lastName>Doe</lastName>\n" +
            "\t</greeting>\n" +
            "\t<greeting>\n" +
            "\t\t<firstName>Some</firstName>\n" +
            "\t\t<lastName>One</lastName>\n" +
            "\t</greeting>\n" +
            "</greetings>";


    @Test public void
    deserializes_single_sub_node_using_jaxb() {
        // When
        final Greeting greeting = from(COOL_GREETING).getObject("cool.greeting", Greeting.class);

        // Then
        assertThat(greeting.getFirstName(), equalTo("John"));
        assertThat(greeting.getLastName(), equalTo("Doe"));
    }

    @Test public void
    deserializes_root_node_using_jaxb() {
        // When
        final CoolGreeting greeting = from(COOL_GREETING).getObject("cool", CoolGreeting.class);

        // Then
        assertThat(greeting.getGreeting().getFirstName(), equalTo("John"));
        assertThat(greeting.getGreeting().getLastName(), equalTo("Doe"));
    }

//    @Test public void
//    deserializes_x_node_using_jaxb() {
//        // When
//        final String greeting = from(COOL_GREETING).getObject("greetings.greeting.firstName", String.class);
//
//        // Then
//        System.out.println(greeting);
//    }
}
