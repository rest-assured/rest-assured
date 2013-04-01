package com.jayway.restassured.path.xml;

import com.jayway.restassured.mapper.ObjectDeserializationContext;
import com.jayway.restassured.path.xml.mapping.XmlPathObjectDeserializer;
import com.jayway.restassured.path.xml.support.CoolGreeting;
import com.jayway.restassured.path.xml.support.Greeting;
import com.jayway.restassured.path.xml.support.Greetings;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jayway.restassured.path.xml.XmlPath.from;
import static com.jayway.restassured.path.xml.config.XmlPathConfig.xmlPathConfig;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class XmlPathObjectDeserializationTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

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

    @Test public void
    deserializes_xml_document_including_list_using_jaxb() {
        // When
        final Greetings greetings = from(GREETINGS).getObject("greetings", Greetings.class);

        // Then
        assertThat(greetings.getGreeting().size(), is(3));
    }

    @Test public void
    cannot_deserialize_list_when_using_getObject() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Failed to convert XML to Java Object. If you're trying convert to a list then use the getList method instead.");

        // When
        final List<Greeting>  greetings = from(GREETINGS).getObject("greetings.greeting", List.class);

        // Then
        assertThat(greetings.size(), is(3));
    }

    @Test public void
    deserializes_list_using_getList() {
        // When
        final List<Greeting>  greetings = from(GREETINGS).getList("greetings.greeting", Greeting.class);

        // Then
        assertThat(greetings.size(), is(3));
    }

    @Test public void
    xml_path_supports_custom_deserializer() {
        // Given
        final AtomicBoolean customDeserializerUsed = new AtomicBoolean(false);

        final XmlPath xmlPath = new XmlPath(COOL_GREETING).using(xmlPathConfig().defaultObjectDeserializer(new XmlPathObjectDeserializer() {
            public <T> T deserialize(ObjectDeserializationContext ctx) {
                customDeserializerUsed.set(true);
                final String xml = ctx.getDataToDeserialize().asString();
                final Greeting greeting = new Greeting();
                greeting.setFirstName(StringUtils.substringBetween(xml, "<firstName>", "</firstName>"));
                greeting.setLastName(StringUtils.substringBetween(xml, "<lastName>", "</lastName>"));
                return (T) greeting;
            }
        }));

        // When
        final Greeting greeting = xmlPath.getObject("", Greeting.class);

        // Then
        assertThat(greeting.getFirstName(), equalTo("John"));
        assertThat(greeting.getLastName(), equalTo("Doe"));
        assertThat(customDeserializerUsed.get(), is(true));
    }

    @Test public void
    xml_path_supports_custom_deserializer_using_static_configuration() {
        // Given
        final AtomicBoolean customDeserializerUsed = new AtomicBoolean(false);

        XmlPath.config = xmlPathConfig().defaultObjectDeserializer(new XmlPathObjectDeserializer() {
            public <T> T deserialize(ObjectDeserializationContext ctx) {
                customDeserializerUsed.set(true);
                final String xml = ctx.getDataToDeserialize().asString();
                final Greeting greeting = new Greeting();
                greeting.setFirstName(StringUtils.substringBetween(xml, "<firstName>", "</firstName>"));
                greeting.setLastName(StringUtils.substringBetween(xml, "<lastName>", "</lastName>"));
                return (T) greeting;
            }
        });

        // When
        try {
            final XmlPath xmlPath = new XmlPath(COOL_GREETING);
            final Greeting greeting = xmlPath.getObject("", Greeting.class);

            // Then
            assertThat(greeting.getFirstName(), equalTo("John"));
            assertThat(greeting.getLastName(), equalTo("Doe"));
            assertThat(customDeserializerUsed.get(), is(true));
        } finally {
            XmlPath.reset();
        }
    }
}
