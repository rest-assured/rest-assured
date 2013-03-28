/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.path.json;

import com.jayway.restassured.path.json.config.JsonPathConfig;
import com.jayway.restassured.path.json.exception.JsonPathException;
import com.jayway.restassured.path.json.support.Book;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.path.json.JsonPath.*;
import static com.jayway.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;
import static com.jayway.restassured.path.json.config.JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class JsonPathTest {

    private final String JSON = "{ \"store\": {\n" +
            "    \"book\": [ \n" +
            "      { \"category\": \"reference\",\n" +
            "        \"author\": \"Nigel Rees\",\n" +
            "        \"title\": \"Sayings of the Century\",\n" +
            "        \"price\": 8.95\n" +
            "      },\n" +
            "      { \"category\": \"fiction\",\n" +
            "        \"author\": \"Evelyn Waugh\",\n" +
            "        \"title\": \"Sword of Honour\",\n" +
            "        \"price\": 12\n" +
            "      },\n" +
            "      { \"category\": \"fiction\",\n" +
            "        \"author\": \"Herman Melville\",\n" +
            "        \"title\": \"Moby Dick\",\n" +
            "        \"isbn\": \"0-553-21311-3\",\n" +
            "        \"price\": 8.99\n" +
            "      },\n" +
            "      { \"category\": \"fiction\",\n" +
            "        \"author\": \"J. R. R. Tolkien\",\n" +
            "        \"title\": \"The Lord of the Rings\",\n" +
            "        \"isbn\": \"0-395-19395-8\",\n" +
            "        \"price\": 22.99\n" +
            "      }\n" +
            "    ],\n" +
            "    \"bicycle\": {\n" +
            "      \"color\": \"red\",\n" +
            "      \"price\": 19.95,\n" +
            "      \"atoms\": "+Long.MAX_VALUE+",\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private final String JSON2 = "[{\"email\":\"name1@mail.com\",\"alias\":\"name one\",\"phone\":\"3456789\"},\n" +
            "{\"email\":\"name2@mail.com\",\"alias\":\"name two\",\"phone\":\"1234567\"},\n" +
            "{\"email\":\"name3@mail.com\",\"alias\":\"name three\",\"phone\":\"2345678\"}]";


    private final String JSON_MAP = "{ \"price1\" : 12.3,\n" +
            "  \"price2\": 15.0 }";

    private final String JSON_PATH_STARTING_WITH_NUMBER = "{ \"0\" : 12.3,\n" +
            "  \"1\": 15.0 }";

    private final String JSON_PATH_WITH_NUMBER = "{ \"map\" : { \"0\" : 12.3,\n" +
            "  \"1\": 15.0 } }";

    private final String JSON_PATH_WITH_BOOLEAN = "{ \"map\" : { \"true\" : 12.3,\n" +
            "  \"false\": 15.0 } }";

    private final String MALFORMED_JSON = "{\n" +
            "    \"a\": 123456\n" +
            "    \"b\":\"string\"\n" +
            "}";

    @Test
    public void getList() throws Exception {
        final List<String> categories = new JsonPath(JSON).get("store.book.category");
        assertThat(categories.size(), equalTo(4));
        assertThat(categories, hasItems("reference", "fiction"));
    }

    @Test
    public void firstBookCategory() throws Exception {
        final String category = with(JSON).get("store.book[0].category");
        assertThat(category, equalTo("reference"));
    }

    @Test
    public void lastBookTitle() throws Exception {
        final String title = with(JSON).get("store.book[-1].title");
        assertThat(title, equalTo("The Lord of the Rings"));
    }

    @Test
    public void booksBetween5And15() throws Exception {
        final List<Map<String, ?>> books = with(JSON).get("store.book.findAll { book -> book.price >= 5 && book.price <= 15 }");
        assertThat(books.size(), equalTo(3));

        final String author = (String) books.get(0).get("author");
        assertThat(author, equalTo("Nigel Rees"));

        final int price = (Integer) books.get(1).get("price");
        assertThat(price, equalTo(12));
    }

    @Test
    public void sizeInPath() throws Exception {
        final Integer size = with(JSON).get("store.book.size()");
        assertThat(size, equalTo(4));
    }

    @Test
    public void getRootObjectAsMap() throws Exception {
        final Map<String, Map> store = given(JSON).get("store");
        assertThat(store.size(), equalTo(2));

        final Map<String, Object> bicycle = store.get("bicycle");
        final String color = (String) bicycle.get("color");
        final float price = (Float) bicycle.get("price");
        assertThat(color, equalTo("red"));
        assertThat(price, equalTo(19.95f));
    }

    @Test
    public void getFloatAndDoublesAsBigDecimal() throws Exception {
        final JsonPath using = with(JSON).using(new JsonPathConfig(BIG_DECIMAL));
        assertThat(using.<Map<String, Map>>get("store").size(), equalTo(2));

        final Map<String, Object> bicycle = using.<Map<String, Map>>get("store").get("bicycle");
        final String color = (String) bicycle.get("color");
        final BigDecimal price = (BigDecimal) bicycle.get("price");
        assertThat(color, equalTo("red"));
        assertThat(price, equalTo(new BigDecimal("19.95")));
    }

    @Test
    public void getFloatAndDoublesAsBigDecimalUsingStaticConfiguration() throws Exception {
        JsonPath.config = new JsonPathConfig().numberReturnType(BIG_DECIMAL);
        try {
            final Map<String, Map> store = with(JSON).get("store");
            assertThat(store.size(), equalTo(2));

            final Map<String, Object> bicycle = store.get("bicycle");
            final String color = (String) bicycle.get("color");
            final BigDecimal price = (BigDecimal) bicycle.get("price");
            assertThat(color, equalTo("red"));
            assertThat(price, equalTo(new BigDecimal("19.95")));
        } finally {
            JsonPath.config = null;
        }
    }

    @Test
    public void nonStaticJsonPathConfigHasPrecedenceOverStaticConfiguration() throws Exception {
        JsonPath.config = new JsonPathConfig().numberReturnType(FLOAT_AND_DOUBLE);
        try {
            final Map<String, Map> store = with(JSON).using(new JsonPathConfig(BIG_DECIMAL)).get("store");
            assertThat(store.size(), equalTo(2));

            final Map<String, Object> bicycle = store.get("bicycle");
            final String color = (String) bicycle.get("color");
            final BigDecimal price = (BigDecimal) bicycle.get("price");
            assertThat(color, equalTo("red"));
            assertThat(price, equalTo(new BigDecimal("19.95")));
        } finally {
            JsonPath.config = null;
        }
    }

    @Test
    public void getRootObjectAsMap2() throws Exception {
        final Map<String, Object> store = from(JSON).get("store.book[0]");
        for (Map.Entry<String, Object> stringObjectEntry : store.entrySet()) {
            System.out.println(stringObjectEntry.getKey() + " = "+stringObjectEntry.getValue());
        }
    }

    @Test
    public void rootPath() throws Exception {
        final JsonPath jsonPath = new JsonPath(JSON).setRoot("store.book");
        assertThat(jsonPath.getInt("size()"), equalTo(4));
        assertThat(jsonPath.getList("author", String.class), hasItem("J. R. R. Tolkien"));

    }

    @Test
    public void supportsGettingEntireObjectGraphUsingEmptyString() throws Exception {
        final List<Map<String, String>> object = from(JSON2).get("");
        assertThat(object.get(0).get("email"), equalTo("name1@mail.com"));
    }

    @Test
    public void supportsGettingEntireObjectGraphUsing$() throws Exception {
        final List<Map<String, String>> object = from(JSON2).get("$");
        assertThat(object.get(0).get("email"), equalTo("name1@mail.com"));
    }

    @Test
    public void supportsGettingEntireObjectGraphUsingNoArgumentGet() throws Exception {
        final List<Map<String, String>> object = from(JSON2).get();
        assertThat(object.get(0).get("email"), equalTo("name1@mail.com"));
    }

    @Test
    public void getValueFromUnnamedRootObject() throws Exception {
        final Map<String, String> object = from(JSON2).get("get(0)");
        assertThat(object.get("email"), equalTo("name1@mail.com"));
    }

    @Test
    public void getValueFromUnnamedRootObjectUsingBrackets() throws Exception {
        final Map<String, String> object = from(JSON2).get("[0]");
        assertThat(object.get("email"), equalTo("name1@mail.com"));
    }

    @Test
    public void getSubValueFromUnnamedRootObjectUsingBrackets() throws Exception {
        final String object = from(JSON2).getString("[0].email");
        assertThat(object, equalTo("name1@mail.com"));
    }

    @Test
    public void getNumericalValues() {
        assertThat(with(JSON).getDouble("store.book[0].price"), equalTo(8.95D));
        assertThat(with(JSON).getFloat("store.book[0].price"), equalTo(8.95F));

        // The price is stored as an integer
        assertThat(with(JSON).getByte("store.book[1].price"), equalTo((byte) 12));
        assertThat(with(JSON).getShort("store.book[1].price"), equalTo((short) 12));
        assertThat(with(JSON).getInt("store.book[1].price"), equalTo(12));
        assertThat(with(JSON).getLong("store.book[1].price"), equalTo(12L));

        // The atoms is stored as a long
        assertThat(with(JSON).getByte("store.bicycle.atoms"), equalTo((byte) Long.MAX_VALUE));
        assertThat(with(JSON).getShort("store.bicycle.atoms"), equalTo((short) Long.MAX_VALUE));
        assertThat(with(JSON).getInt("store.bicycle.atoms"), equalTo((int) Long.MAX_VALUE));
        assertThat(with(JSON).getLong("store.bicycle.atoms"), equalTo(Long.MAX_VALUE));
    }

    @Test
    public void convertsValueToStringWhenExplicitlyRequested() throws Exception {
        String phoneNumber = from(JSON2).getString("phone[0]");

        assertThat(phoneNumber, equalTo("3456789"));
    }

    @Test
    public void convertsValueToIntWhenExplicitlyRequested() throws Exception {
        int phoneNumber = from(JSON2).getInt("phone[0]");

        assertThat(phoneNumber, equalTo(3456789));
    }

    @Test
    public void convertsValueToDoubleWhenExplicitlyRequested() throws Exception {
        double phoneNumber = from(JSON2).getDouble("phone[0]");

        assertThat(phoneNumber, equalTo(3456789d));
    }

    @Test
    public void convertsValueToFloatWhenExplicitlyRequested() throws Exception {
        float phoneNumber = from(JSON2).getFloat("phone[0]");

        assertThat(phoneNumber, equalTo(3456789f));
    }

    @Test
    public void convertsListMembersToDefinedTypeIfPossible() throws Exception {
        final List<Integer> phoneNumbers = with(JSON2).getList("phone", int.class);

        assertThat(phoneNumbers, hasItems(3456789, 1234567, 2345678));
    }

    @Test
    public void getMapWithGenericType() throws Exception {
        final Map<String, String> map = with(JSON_MAP).getMap("$", String.class, String.class);

        assertThat(map, allOf(hasEntry("price1", "12.3"), hasEntry("price2", "15.0")));
    }

    @Test
    public void getMapWithAnotherGenericType() throws Exception {
        final Map<String, Float> map = with(JSON_MAP).getMap("$", String.class, float.class);

        assertThat(map, allOf(hasEntry("price1", 12.3f), hasEntry("price2", 15.0f)));
    }

    @Test
    public void getStringConvertsTheResultToAString() throws Exception {
        final String priceAsString = with(JSON).getString("store.book.price[0]");

        assertThat(priceAsString, is("8.95"));
    }

    @Test(expected = JsonPathException.class)
    public void malformedJson() throws Exception {
        from(MALFORMED_JSON).get("a");
    }

    @Test
    public void getObjectWorksWhenPathPointsToAJsonObject() throws Exception {
        final Book book = from(JSON).getObject("store.book[2]", Book.class);

        assertThat(book, equalTo(new Book("fiction", "Herman Melville", "Moby Dick", "0-553-21311-3", 8.99f)));
    }

    @Test
    public void getObjectAsMapWorksWhenPathPointsToAJsonObject() throws Exception {
        final Map<String, String> book = from(JSON).getObject("store.book[2]", Map.class);

        assertThat(book, hasEntry("category", "fiction"));
        assertThat(book, hasEntry("author", "Herman Melville"));
    }

    @Test
    public void getObjectWorksWhenPathPointsToAList() throws Exception {
        final List<String> categories = from(JSON).getObject("store.book.category", List.class);

        assertThat(categories, hasItems("reference", "fiction"));
    }

    @Test
    public void getObjectAsFloatWorksWhenPathPointsToAFloat() throws Exception {
        final Float price = from(JSON).getObject("store.book.price[0]", Float.class);

        assertThat(price, equalTo(8.95f));
    }

    @Test
    public void getObjectAsStringWorksWhenPathPointsToAString() throws Exception {
        final String category = from(JSON).getObject("store.book.category[0]", String.class);

        assertThat(category, equalTo("reference"));
    }

    @Test
    public void jsonPathSupportsPrettifiyingJson() throws Exception {
        final String prettyJson = with(JSON2).prettify();

        assertThat(prettyJson, equalTo("[\n    {\n        \"phone\": \"3456789\",\n        \"alias\": \"name one\",\n        \"email\": \"name1@mail.com\"\n    },\n    {\n        \"phone\": \"1234567\",\n        \"alias\": \"name two\",\n        \"email\": \"name2@mail.com\"\n    },\n    {\n        \"phone\": \"2345678\",\n        \"alias\": \"name three\",\n        \"email\": \"name3@mail.com\"\n    }\n]"));
    }

    @Test
    public void jsonPathSupportsPrettyPrintingJson() throws Exception {
        final String prettyJson = with(JSON2).prettyPrint();

        assertThat(prettyJson, equalTo("[\n    {\n        \"phone\": \"3456789\",\n        \"alias\": \"name one\",\n        \"email\": \"name1@mail.com\"\n    },\n    {\n        \"phone\": \"1234567\",\n        \"alias\": \"name two\",\n        \"email\": \"name2@mail.com\"\n    },\n    {\n        \"phone\": \"2345678\",\n        \"alias\": \"name three\",\n        \"email\": \"name3@mail.com\"\n    }\n]"));
    }

    @Test
    public void canParseJsonDocumentWhenFirstKeyIsIntegerUsingManualEscaping() throws Exception {
        final float number = from(JSON_PATH_STARTING_WITH_NUMBER).getFloat("'0'");

        assertThat(number, equalTo(12.3f));
    }

    @Test
    public void canParseJsonDocumentWhenFirstKeyThatIsAIntegerUsingNoEscaping() throws Exception {
        final float number = from(JSON_PATH_STARTING_WITH_NUMBER).getFloat("0");

        assertThat(number, equalTo(12.3f));
    }

    @Test
    public void canParseJsonDocumentWhenPathIncludesKeyThatIsAIntegerUsingNoEscaping() throws Exception {
        final float number = from(JSON_PATH_WITH_NUMBER).getFloat("map.0");

        assertThat(number, equalTo(12.3f));
    }

    @Test
    public void canParseJsonDocumentWhenPathIncludesKeyThatIsABooleanUsingEscaping() throws Exception {
        final float number = from(JSON_PATH_WITH_BOOLEAN).getFloat("map.'false'");

        assertThat(number, equalTo(15.0f));
    }

    @Test
    public void canParseJsonDocumentWhenPathIncludesKeyThatIsABooleanUsingNoEscaping() throws Exception {
        final float number = from(JSON_PATH_WITH_BOOLEAN).getFloat("map.true");

        assertThat(number, equalTo(12.3f));
    }

    @Test
    public void canParseJsonDocumentWhenPathIncludesMinusInsideEscaped() throws Exception {
        JsonPath path = new JsonPath("{ \"a-b\"  : \"minus\" , \"a.b\" : \"dot\"  , \"a.b-c\" : \"both\"  }" );

        assertThat(path.getString("'a.b-c'"), equalTo("both"));
    }

    /**
     * Verifies that issue 195 is resolved.
     */
    @Test
    public void canParseJsonDocumentWithMultipleConsecutiveIntegersInsidePath() throws Exception {
        String json = "{\n" +
                "    \"foo.bar.baz\": {\n" +
                "        \"0.2.0\": \"test\"\n" +
                "    }\n" +
                "}";

        final String string = from(json).getString("'foo.bar.baz'.'0.2.0'");

        assertThat(string, equalTo("test"));
    }
}
