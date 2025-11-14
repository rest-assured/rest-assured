package io.restassured.internal.support;

import io.restassured.parsing.Parser;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static junit.framework.TestCase.assertEquals;

public class PrettifierTest {

    private File jsonFile;
    private File xmlFile;
    private final String resultJson = "{\n" +
            "    \"catalog\": {\n" +
            "        \"book\": [\n" +
            "            {\n" +
            "                \"id\": \"bk101\",\n" +
            "                \"author\": \"Gambardella, Matthew\",\n" +
            "                \"title\": \"XML Developer's Guide\",\n" +
            "                \"genre\": \"Computer\",\n" +
            "                \"price\": 44.95,\n" +
            "                \"publish_date\": \"2000-10-01\"\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}";

    private final String resultXml = "<catalog>\n" +
            "  <book id=\"bk101\">\n" +
            "    <author>Gambardella, Matthew</author>\n" +
            "    <title>XML Developer's Guide</title>\n" +
            "    <genre>Computer</genre>\n" +
            "    <price>44.95</price>\n" +
            "    <publish_date>2000-10-01</publish_date>\n" +
            "  </book>\n" +
            "</catalog>";


    @BeforeEach
    public void
    setup() throws IOException {
        jsonFile = getFile("multipart.json");
        xmlFile = getFile("multipart.xml");
    }

    @Test
    public void
    jsonPrettify() {
        String prettify = new Prettifier().prettify(jsonFile, Parser.JSON);

        assertEquals(prettify, resultJson);
    }

    @Test
    public void
    xml_prettify() {
        String prettify = new Prettifier().prettify(xmlFile, Parser.XML);

        assertEquals(prettify, resultXml);
    }

    @Test
    public void
    json_prettify() {
        String prettify = new Prettifier().prettify(jsonFile, Parser.JSON);

        assertEquals(prettify, resultJson);
    }

    @Test
    public void
    empty_data() {
        String prettify = new Prettifier().prettify("", Parser.XML);

        assertEquals(prettify, "");
    }

    @Test
    public void
    json_string_data() throws IOException {
        String jsonData = new String(Files.readAllBytes(Paths.get(jsonFile.getPath())));

        String prettify = new Prettifier().prettify(jsonData, Parser.JSON);

        assertEquals(prettify, resultJson);
    }

    @Test
    public void
    xml_string_data() throws IOException {
        String xmlData = new String(Files.readAllBytes(Paths.get(xmlFile.getPath())));

        String prettify = new Prettifier().prettify(xmlData, Parser.XML);

        assertEquals(prettify, resultXml);
    }

    private File getFile(String path) throws IOException {
        return new File(getClass().getClassLoader().getResource(path).getFile());
    }
}
