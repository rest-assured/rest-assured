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

package io.restassured.path.json;

import io.restassured.common.mapper.TypeRef;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.path.json.support.Book;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

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
            "      \"atoms\": " + Long.MAX_VALUE + ",\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private final String JSON2 = "[{\"email\":\"name1@mail.com\",\"alias\":\"name one\",\"phone\":\"3456789\"},\n" +
            "{\"email\":\"name2@mail.com\",\"alias\":\"name two\",\"phone\":\"1234567\"},\n" +
            "{\"email\":\"name3@mail.com\",\"alias\":\"name three\",\"phone\":\"2345678\"}]";

    private final String JSON3 = "{\"id\":\"db24eeeb-7fe5-41d3-8f06-986b793ecc91\"}";


    private final String JSON_MAP = "{ \"price1\" : 12.3,\n" +
            "  \"price2\": 15.0 }";

    private final String JSON_PATH_STARTING_WITH_NUMBER = "{ \"0\" : 12.3,\n" +
            "  \"1\": 15.0 }";

    private final String JSON_PATH_WITH_NUMBER = "{ \"map\" : { \"0\" : 12.3,\n" +
            "  \"1\": 15.0 } }";

    private final String JSON_PATH_WITH_SIZE = "{ \"map\" : { \"size\" : 12.3,\n" +
            "  \"1\": 15.0 } }";

    private final String JSON_PATH_WITH_BOOLEAN = "{ \"map\" : { \"true\" : 12.3,\n" +
            "  \"false\": 15.0 } }";

    private final String MALFORMED_JSON = "{\n" +
            "    \"a\": 123456\n" +
            "    \"b\":\"string\"\n" +
            "}";

    @Test
    public void getList() {
        final List<String> categories = new JsonPath(JSON).get("store.book.category");
        assertThat(categories.size(), equalTo(4));
        assertThat(categories, hasItems("reference", "fiction"));
    }

    @Test
    public void firstBookCategory() {
        final String category = JsonPath.with(JSON).get("store.book[0].category");
        assertThat(category, equalTo("reference"));
    }

    @Test
    public void lastBookTitle() {
        final String title = JsonPath.with(JSON).get("store.book[-1].title");
        assertThat(title, equalTo("The Lord of the Rings"));
    }

    @Test
    public void booksWithArgAuthor() {
        String author = "Herman Melville";
        final List<Map<String, ?>> books = JsonPath.with(JSON)
                .param("author", author)
                .get("store.book.findAll { book -> book.author == author }");
        assertThat(books.size(), equalTo(1));

        final String authorActual = (String) books.get(0).get("author");
        assertThat(authorActual, equalTo(author));
    }

    @Test
    public void booksBetween5And15() {
        final List<Map<String, ?>> books = JsonPath.with(JSON).get("store.book.findAll { book -> book.price >= 5 && book.price <= 15 }");
        assertThat(books.size(), equalTo(3));

        final String author = (String) books.get(0).get("author");
        assertThat(author, equalTo("Nigel Rees"));

        final int price = (Integer) books.get(1).get("price");
        assertThat(price, equalTo(12));
    }

    @Test
    public void sizeInPath() {
        final Integer size = JsonPath.with(JSON).get("store.book.size()");
        assertThat(size, equalTo(4));
    }

    @Test
    public void getRootObjectAsMap() {
        final Map<String, Map> store = JsonPath.given(JSON).get("store");
        assertThat(store.size(), equalTo(2));

        final Map<String, Object> bicycle = store.get("bicycle");
        final String color = (String) bicycle.get("color");
        final float price = (Float) bicycle.get("price");
        assertThat(color, equalTo("red"));
        assertThat(price, equalTo(19.95f));
    }

    @Test
    public void getFloatAndDoublesAsBigDecimal() {
        final JsonPath using = JsonPath.with(JSON).using(new JsonPathConfig(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
        assertThat(using.<Map<String, Map>>get("store").size(), equalTo(2));

        final Map<String, Object> bicycle = using.<Map<String, Map>>get("store").get("bicycle");
        final String color = (String) bicycle.get("color");
        final BigDecimal price = (BigDecimal) bicycle.get("price");
        assertThat(color, equalTo("red"));
        assertThat(price, equalTo(new BigDecimal("19.95")));
    }

    @Test
    public void getFloatAndDoublesAsBigDecimalUsingStaticConfiguration() {
        JsonPath.config = new JsonPathConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL);
        try {
            final Map<String, Map> store = JsonPath.with(JSON).get("store");
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
    public void nonStaticJsonPathConfigHasPrecedenceOverStaticConfiguration() {
        JsonPath.config = new JsonPathConfig().numberReturnType(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE);
        try {
            final Map<String, Map> store = JsonPath.with(JSON).using(new JsonPathConfig(JsonPathConfig.NumberReturnType.BIG_DECIMAL)).get("store");
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
    public void getRootObjectAsMap2() {
        final Map<String, Object> store = JsonPath.from(JSON).get("store.book[0]");
        for (Map.Entry<String, Object> stringObjectEntry : store.entrySet()) {
            System.out.println(stringObjectEntry.getKey() + " = " + stringObjectEntry.getValue());
        }
    }

    @Test
    public void rootPath() {
        final JsonPath jsonPath = new JsonPath(JSON).setRootPath("store.book");
        assertThat(jsonPath.getInt("size()"), equalTo(4));
        assertThat(jsonPath.getList("author", String.class), hasItem("J. R. R. Tolkien"));
    }

    @Test
    public void rootPathFollowedByArrayIndexing() {
        final JsonPath jsonPath = new JsonPath(JSON).setRootPath("store.book");
        assertThat(jsonPath.getString("[0].author"), equalTo("Nigel Rees"));
    }

    @Test
    public void supportsGettingEntireObjectGraphUsingEmptyString() {
        final List<Map<String, String>> object = JsonPath.from(JSON2).get("");
        assertThat(object.get(0).get("email"), equalTo("name1@mail.com"));
    }

    @Test
    public void supportsGettingEntireObjectGraphUsing$() {
        final List<Map<String, String>> object = JsonPath.from(JSON2).get("$");
        assertThat(object.get(0).get("email"), equalTo("name1@mail.com"));
    }

    @Test
    public void supportsGettingEntireObjectGraphUsingNoArgumentGet() {
        final List<Map<String, String>> object = JsonPath.from(JSON2).get();
        assertThat(object.get(0).get("email"), equalTo("name1@mail.com"));
    }

    @Test
    public void getValueFromUnnamedRootObject() {
        final Map<String, String> object = JsonPath.from(JSON2).get("get(0)");
        assertThat(object.get("email"), equalTo("name1@mail.com"));
    }

    @Test
    public void getValueFromUnnamedRootObjectUsingBrackets() {
        final Map<String, String> object = JsonPath.from(JSON2).get("[0]");
        assertThat(object.get("email"), equalTo("name1@mail.com"));
    }

    @Test
    public void getSubValueFromUnnamedRootObjectUsingBrackets() {
        final String object = JsonPath.from(JSON2).getString("[0].email");
        assertThat(object, equalTo("name1@mail.com"));
    }

    @Test
    public void getLastValueFromUnnamedRootObjectUsingBrackets() {
        final Map<String, String> object = JsonPath.from(JSON2).get("[-1]");
        assertThat(object.get("email"), equalTo("name3@mail.com"));
    }

    @Test
    public void getLastSubValueFromUnnamedRootObjectUsingBrackets() {
        final String object = JsonPath.from(JSON2).getString("[-1].email");
        assertThat(object, equalTo("name3@mail.com"));
    }

    @Test
    public void getNumericalValues() {
        assertThat(JsonPath.with(JSON).getDouble("store.book[0].price"), equalTo(8.95D));
        assertThat(JsonPath.with(JSON).getFloat("store.book[0].price"), equalTo(8.95F));

        // The price is stored as an integer
        assertThat(JsonPath.with(JSON).getByte("store.book[1].price"), equalTo((byte) 12));
        assertThat(JsonPath.with(JSON).getShort("store.book[1].price"), equalTo((short) 12));
        assertThat(JsonPath.with(JSON).getInt("store.book[1].price"), equalTo(12));
        assertThat(JsonPath.with(JSON).getLong("store.book[1].price"), equalTo(12L));

        // The atoms is stored as a long
        assertThat(JsonPath.with(JSON).getByte("store.bicycle.atoms"), equalTo((byte) Long.MAX_VALUE));
        assertThat(JsonPath.with(JSON).getShort("store.bicycle.atoms"), equalTo((short) Long.MAX_VALUE));
        assertThat(JsonPath.with(JSON).getInt("store.bicycle.atoms"), equalTo((int) Long.MAX_VALUE));
        assertThat(JsonPath.with(JSON).getLong("store.bicycle.atoms"), equalTo(Long.MAX_VALUE));
    }

    @Test
    public void convertsValueToStringWhenExplicitlyRequested() {
        String phoneNumber = JsonPath.from(JSON2).getString("phone[0]");

        assertThat(phoneNumber, equalTo("3456789"));
    }

    @Test
    public void convertsValueToIntWhenExplicitlyRequested() {
        int phoneNumber = JsonPath.from(JSON2).getInt("phone[0]");

        assertThat(phoneNumber, equalTo(3456789));
    }

    @Test
    public void convertsValueToDoubleWhenExplicitlyRequested() {
        double phoneNumber = JsonPath.from(JSON2).getDouble("phone[0]");

        assertThat(phoneNumber, equalTo(3456789d));
    }

    @Test
    public void convertsValueToFloatWhenExplicitlyRequested() {
        float phoneNumber = JsonPath.from(JSON2).getFloat("phone[0]");

        assertThat(phoneNumber, equalTo(3456789f));
    }

    @Test
    public void convertsValueToUUIDWhenExplicitlyRequested() {
        UUID uuid = JsonPath.from(JSON3).getUUID("id");

        assertThat(uuid, equalTo(UUID.fromString("db24eeeb-7fe5-41d3-8f06-986b793ecc91")));
    }

    @Test
    public void convertsListMembersToDefinedTypeIfPossible() {
        final List<Integer> phoneNumbers = JsonPath.with(JSON2).getList("phone", int.class);

        assertThat(phoneNumbers, hasItems(3456789, 1234567, 2345678));
    }

    @Test
    public void getMapWithGenericType() {
        final Map<String, String> map = JsonPath.with(JSON_MAP).getMap("$", String.class, String.class);

        assertThat(map, allOf(hasEntry("price1", "12.3"), hasEntry("price2", "15.0")));
    }

    @Test
    public void getMapWithAnotherGenericType() {
        final Map<String, Float> map = JsonPath.with(JSON_MAP).getMap("$", String.class, float.class);

        assertThat(map, allOf(hasEntry("price1", 12.3f), hasEntry("price2", 15.0f)));
    }

    @Test
    public void getStringConvertsTheResultToAString() {
        final String priceAsString = JsonPath.with(JSON).getString("store.book.price[0]");

        assertThat(priceAsString, is("8.95"));
    }

    @Test
    public void malformedJson() {
        Throwable throwable = catchThrowable(() -> JsonPath.from(MALFORMED_JSON).get("a"));

        Assertions.assertThat(throwable).isInstanceOf(JsonPathException.class);
    }

    @Test
    public void getObjectWorksWhenPathPointsToAJsonObject() {
        final Book book = JsonPath.from(JSON).getObject("store.book[2]", Book.class);

        assertThat(book, equalTo(new Book("fiction", "Herman Melville", "Moby Dick", "0-553-21311-3", 8.99f)));
    }

    @Test
    public void getObjectWorksWhenPathPointsToATypeRefMap() {
        final Map<String, Object> book = JsonPath.from(JSON).getObject("store.book[2]", new TypeRef<Map<String, Object>>() {});

        assertThat(book.get("category"), Matchers.<Object>equalTo("fiction"));
        assertThat(book.get("author"), Matchers.<Object>equalTo("Herman Melville"));
        assertThat(book.get("price"), Matchers.<Object>equalTo(8.99));
    }

    @Test
    public void getObjectWorksWhenPathPointsToATypeRefList() {
        final List<Float> prices = JsonPath.from(JSON).getObject("store.book.price", new TypeRef<List<Float>>() {});

        assertThat(prices, containsInAnyOrder(8.95, 12, 8.99, 22.99));
    }

    @Test
    public void getObjectWorksWhenPathPointsToAJsonObject2() {
        final List<Book> books = JsonPath.from(JSON).getList("store.book", Book.class);

        assertThat(books, hasSize(4));
        assertThat(books.get(0).getAuthor(), equalTo("Nigel Rees"));
    }

    @Test
    public void getObjectAsMapWorksWhenPathPointsToAJsonObject() {
        final Map<String, String> book = JsonPath.from(JSON).getObject("store.book[2]", Map.class);

        assertThat(book, hasEntry("category", "fiction"));
        assertThat(book, hasEntry("author", "Herman Melville"));
    }

    @Test
    public void getObjectWorksWhenPathPointsToAList() {
        final List<String> categories = JsonPath.from(JSON).getObject("store.book.category", List.class);

        assertThat(categories, hasItems("reference", "fiction"));
    }

    @Test
    public void getObjectAsFloatWorksWhenPathPointsToAFloat() {
        final Float price = JsonPath.from(JSON).getObject("store.book.price[0]", Float.class);

        assertThat(price, equalTo(8.95f));
    }

    @Test
    public void getObjectAsStringWorksWhenPathPointsToAString() {
        final String category = JsonPath.from(JSON).getObject("store.book.category[0]", String.class);

        assertThat(category, equalTo("reference"));
    }

    @Test
    public void jsonPathSupportsPrettifiyingJson() {
        final String prettyJson = JsonPath.with(JSON2).prettify();

        assertThat(prettyJson, equalTo("[\n    {\n        \"email\": \"name1@mail.com\",\n        \"alias\": \"name one\",\n        \"phone\": \"3456789\"\n    },\n    {\n        \"email\": \"name2@mail.com\",\n        \"alias\": \"name two\",\n        \"phone\": \"1234567\"\n    },\n    {\n        \"email\": \"name3@mail.com\",\n        \"alias\": \"name three\",\n        \"phone\": \"2345678\"\n    }\n]"));

    }

    @Test
    public void jsonPathSupportsPrettyPrintingJson() {
        final String prettyJson = JsonPath.with(JSON2).prettyPrint();

        assertThat(prettyJson, equalTo("[\n    {\n        \"email\": \"name1@mail.com\",\n        \"alias\": \"name one\",\n        \"phone\": \"3456789\"\n    },\n    {\n        \"email\": \"name2@mail.com\",\n        \"alias\": \"name two\",\n        \"phone\": \"1234567\"\n    },\n    {\n        \"email\": \"name3@mail.com\",\n        \"alias\": \"name three\",\n        \"phone\": \"2345678\"\n    }\n]"));
    }

    @Test
    public void jsonPathSupportsPrettyPeekingJson() {
        final String phone = JsonPath.with(JSON2).prettyPeek().getString("phone[0]");

        assertThat(phone, equalTo("3456789"));
    }

    @Test
    public void jsonPathSupportsPeekingAtTheJson() {
        final String phone = JsonPath.with(JSON2).peek().getString("phone[0]");

        assertThat(phone, equalTo("3456789"));
    }

    @Test
    public void canParseJsonDocumentWhenFirstKeyIsIntegerUsingManualEscaping() {
        final float number = JsonPath.from(JSON_PATH_STARTING_WITH_NUMBER).getFloat("'0'");

        assertThat(number, equalTo(12.3f));
    }

    @Test
    public void canParseJsonDocumentWhenFirstKeyThatIsAIntegerUsingNoEscaping() {
        final float number = JsonPath.from(JSON_PATH_STARTING_WITH_NUMBER).getFloat("0");

        assertThat(number, equalTo(12.3f));
    }

    @Test
    public void canParseJsonDocumentWhenPathIncludesKeyThatIsAIntegerUsingNoEscaping() {
        final float number = JsonPath.from(JSON_PATH_WITH_NUMBER).getFloat("map.0");

        assertThat(number, equalTo(12.3f));
    }

    @Test
    public void canParseJsonDocumentWhenPathIncludesKeyThatIsABooleanUsingEscaping() {
        final float number = JsonPath.from(JSON_PATH_WITH_BOOLEAN).getFloat("map.'false'");

        assertThat(number, equalTo(15.0f));
    }

    @Test
    public void canParseJsonDocumentWhenPathIncludesKeyThatIsABooleanUsingNoEscaping() {
        final float number = JsonPath.from(JSON_PATH_WITH_BOOLEAN).getFloat("map.true");

        assertThat(number, equalTo(12.3f));
    }

    @Test
    public void canParseJsonDocumentWhenPathIncludesMinusInsideEscaped() {
        JsonPath path = new JsonPath("{ \"a-b\"  : \"minus\" , \"a.b\" : \"dot\"  , \"a.b-c\" : \"both\"  }");

        assertThat(path.getString("'a.b-c'"), equalTo("both"));
    }

    /**
     * Verifies that issue 195 is resolved.
     */
    @Test
    public void canParseJsonDocumentWithMultipleConsecutiveIntegersInsidePath() {
        String json = "{\n" +
                "    \"foo.bar.baz\": {\n" +
                "        \"0.2.0\": \"test\"\n" +
                "    }\n" +
                "}";

        final String string = JsonPath.from(json).getString("'foo.bar.baz'.'0.2.0'");

        assertThat(string, equalTo("test"));
    }

    @Test
    public void
    can_parse_multiple_values() {
        // Given
        final JsonPath jsonPath = new JsonPath(JSON);

        // When
        final String category1 = jsonPath.getString("store.book.category[0]");
        final String category2 = jsonPath.getString("store.book.category[1]");

        // Then
        assertThat(category1, equalTo("reference"));
        assertThat(category2, equalTo("fiction"));
    }

    @Test
    public void
    pretty_printing_works() {
        // Given
        String json = "{\"data\": [{" +
                "         \"uid\": 10,\n" +
                "         \"name\": \"abc\"\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        // When
        final JsonPath jsonPath = new JsonPath(json);

        // Then
        final String string = jsonPath.prettyPrint();
        assertThat(string, equalTo("{\n" +
                "    \"data\": [\n" +
                "        {\n" +
                "            \"uid\": 10,\n" +
                "            \"name\": \"abc\"\n" +
                "        }\n" +
                "    ]\n" +
                "}"));
    }

    @Test
    public void
    parses_json_document_with_attribute_name_equal_to_properties() {
        // Given
        final String jsonWithPropertyAttribute = "[{\"properties\":\"test\"}]"; // properties is a reserved word in Groovy

        // When
        final String value = new JsonPath(jsonWithPropertyAttribute).getString("[0].properties");

        // Then
        assertThat(value, equalTo("test"));
    }

    @Test
    public void
    parses_json_document_with_attribute_name_equal_to_size() {
        // When
        final float anInt = new JsonPath(JSON_PATH_WITH_SIZE).getFloat("map.size");

        // Then
        assertThat(anInt, is(12.3f));
    }

    @Test public void
    can_find_if_a_key_exists_in_json_object() {
        // Given
        String json = "{\n" +
                "\"status\": \"success\",\n" +
                "\"fund_code\":\"00200\",\n" +
                "\"fund_name\":\"My Fund Name\",\n" +
                "\"models\": [\n" +
                "  {\n" +
                "    \"model_id\": 639506,\n" +
                "    \"model_name\": \"New Validated Model\",\n" +
                "    \"model_type\": null,\n" +
                "    \"portfolios\": null,\n" +
                "    \"created_date\": 1390978800000,\n" +
                "    \"display_create_date\": \"01/29/2014\",\n" +
                "    \"updated_date\": 1392274800000,\n" +
                "    \"display_update_date\": \"02/13/2014\",\n" +
                "    \"number_of_clients\": 1,\n" +
                "    \"risk\": \"N/A\",\n" +
                "    \"time_frame\": \"N/A\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"model_id\": 639507,\n" +
                "    \"model_name\": \"My Validated Model\",\n" +
                "    \"model_type\": null,\n" +
                "    \"portfolios\": null,\n" +
                "    \"created_date\": 1390978800000,\n" +
                "    \"display_create_date\": \"01/29/2014\",\n" +
                "    \"updated_date\": 1392274800000,\n" +
                "    \"display_update_date\": \"02/13/2014\",\n" +
                "    \"number_of_clients\": 1,\n" +
                "    \"risk\": \"N/A\",\n" +
                "    \"time_frame\": \"N/A\"\n" +
                "  }\n" +
                "]\n" +
                "}";

        // When
        JsonPath jsonPath = new JsonPath(json);

        // Then
        assertThat(jsonPath.getBoolean("any { it.key == 'fund_code' }"), is(true));
        assertThat(jsonPath.getBoolean("models.any { it.containsKey('number_of_clients') }"), is(true));
    }

    @Test public void
    can_parse_json_attributes_starting_with_a_number() {
        // Given
        String json = "{\n" +
                "   \"6269f15a0bb9b1b7d86ae718e84cddcd\" : {\n" +
                "            \"attr1\":\"val1\",\n" +
                "            \"attr2\":\"val2\",\n" +
                "            \"attrx\":\"valx\"\n" +
                "   }\n" +
                "}";

        // When
        JsonPath jsonPath = new JsonPath(json);

        // Then
        assertThat(jsonPath.getString("6269f15a0bb9b1b7d86ae718e84cddcd.attr1"), equalTo("val1"));
    }

    @Test public void
    automatically_escapes_json_attributes_whose_name_equals_properties() {
        // Given
        String json = """
                {
                   "features":[
                      {
                         "type":"Feature",
                         "geometry":{
                            "type":"GeometryCollection",
                            "geometries":[
                               {
                                  "type":"Point",
                                  "coordinates":[
                                     19.883992823270653,
                                     50.02026203045478
                                  ]
                               }
                            ]
                         },
                         "properties":{
                            "gridId":6
                         }
                      },
                      {
                         "type":"Feature",
                         "geometry":{
                            "type":"GeometryCollection",
                            "geometries":[
                               {
                                  "type":"Point",
                                  "coordinates":[
                                     19.901266347582094,
                                     50.07074684071764
                                  ]
                               }
                            ]
                         },
                         "properties":{
                            "gridId":7
                         }
                      }
                   ]
                }""";
        // When
        JsonPath jsonPath = new JsonPath(json);

        // Then
        assertThat(jsonPath.getList("features.properties.gridId", Integer.class), hasItems(7));
    }

    @Test public void
    can_manually_escape_class_property() {
        // Given
        String json = "{\n" +
                "  \"semester\": \"Fall 2015\",\n" +
                "  \"groups\": [\n" +
                "    {\n" +
                "      \"siteUrl\": \"http://cphbusinessjb.cloudapp.net/CA2/\",\n" +
                "      \"error\": \"NO AUTHOR/CLASS-INFO\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"siteUrl\": \"http://ca2-ebski.rhcloud.com/CA2New/\",\n" +
                "      \"authors\": \"Ebbe, Kasper, Christoffer\",\n" +
                "      \"class\": \"A klassen\",\n" +
                "      \"group\": \"Gruppe: Johns Llama Herders A/S\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"siteUrl\": \"http://ca2-chrislind.rhcloud.com/CA2Final/\",\n" +
                "      \"error\": \"NO AUTHOR/CLASS-INFO\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"siteUrl\": \"http://ca2-pernille.rhcloud.com/NYCA2/\",\n" +
                "      \"authors\": \"Marta, Jeanette, Pernille\",\n" +
                "      \"class\": \"DAT A\",\n" +
                "      \"group\": \"Group: MJP\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"siteUrl\": \"https://ca2-afn.rhcloud.com:8443/company.jsp\",\n" +
                "      \"error\": \"NO AUTHOR/CLASS-INFO\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"siteUrl\": \"http://ca-smcphbusiness.rhcloud.com/ca2/index.jsp\",\n" +
                "      \"authors\": \"Mikkel, Steffen, B Andersen\",\n" +
                "      \"class\": \"A Class Computer Science\",\n" +
                "      \"group\": \"1\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        // When
        JsonPath jsonPath = new JsonPath(json);

        // Then
        assertThat(jsonPath.getList("groups.getAt('class')", String.class), hasItems("A klassen"));
    }

    @Test public void
    automatically_escapes_class_property() {
        // Given
        String json = "{\n" +
                "  \"semester\": \"Fall 2015\",\n" +
                "  \"groups\": [\n" +
                "    {\n" +
                "      \"siteUrl\": \"http://cphbusinessjb.cloudapp.net/CA2/\",\n" +
                "      \"error\": \"NO AUTHOR/CLASS-INFO\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"siteUrl\": \"http://ca2-ebski.rhcloud.com/CA2New/\",\n" +
                "      \"authors\": \"Ebbe, Kasper, Christoffer\",\n" +
                "      \"class\": \"A klassen\",\n" +
                "      \"group\": \"Gruppe: Johns Llama Herders A/S\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"siteUrl\": \"http://ca2-chrislind.rhcloud.com/CA2Final/\",\n" +
                "      \"error\": \"NO AUTHOR/CLASS-INFO\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"siteUrl\": \"http://ca2-pernille.rhcloud.com/NYCA2/\",\n" +
                "      \"authors\": \"Marta, Jeanette, Pernille\",\n" +
                "      \"class\": \"DAT A\",\n" +
                "      \"group\": \"Group: MJP\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"siteUrl\": \"https://ca2-afn.rhcloud.com:8443/company.jsp\",\n" +
                "      \"error\": \"NO AUTHOR/CLASS-INFO\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"siteUrl\": \"http://ca-smcphbusiness.rhcloud.com/ca2/index.jsp\",\n" +
                "      \"authors\": \"Mikkel, Steffen, B Andersen\",\n" +
                "      \"class\": \"A Class Computer Science\",\n" +
                "      \"group\": \"1\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        // When
        JsonPath jsonPath = new JsonPath(json);

        // Then
        assertThat(jsonPath.getList("groups.class", String.class), hasItems("A klassen"));
    }

    /**
     * Asserts that https://github.com/jayway/rest-assured/issues/556 is resolved
     */
    @Test public void
    unicode_json_values_are_pretty_printed_without_unicode_escaping() {
        final String prettyJson = JsonPath.with("{\"some\":\"ŘÍŠŽŤČÝŮŇÚĚĎÁÉÓ\"}").prettyPrint();

        assertThat(prettyJson, equalTo("{\n    \"some\": \"ŘÍŠŽŤČÝŮŇÚĚĎÁÉÓ\"\n}"));
    }

    @Test public void
    need_to_escape_lists_with_hyphen_and_brackets() {
        // Given
        String json = "{ \"some-list[0]\" : [ \"one\", \"two\" ] }";

        // When
        JsonPath jsonPath = JsonPath.from(json);

        // Then
        assertThat(jsonPath.getString("'some-list[0]'[0]"), equalTo("one"));
    }

    @Test public void
    doesnt_need_to_escape_lists_with_hyphen_without_brackets() {
        // Given
        String json = "{ \"some-list\" : [ \"one\", \"two\" ] }";

        // When
        JsonPath jsonPath = JsonPath.from(json);

        // Then
        assertThat(jsonPath.getString("some-list[0]"), equalTo("one"));
    }


    /** Tests #1543 is fixed    */
    @Test public void
    does_not_fail_on_absent_lists() {
        // Given
        String json = "{ \"root\" : { } }";

        // When
        JsonPath jsonPath = JsonPath.from(json);

        // Then   no exception should be thrown
        assertThat(jsonPath.getString("root.items[0]"), is(nullValue()));
    }

    /** Tests #1746 is fixed plus additional verifications to ensure we did not break existing behavior */
    @Test public void
    returns_null_for_absent_json_keys(){

        // ------- Given
        String json = "{\"root\": {" +
                      "    \"nKey\": null," +
                      "    \"iKey\": 1," +
                      "    \"fKey\": 1.1," +
                      "    \"sKey\": \"ss\"," +
                      "    \"bKey\": true," +

                      "    \"array\"  : [{" +
                      "        \"nKey\": null," +
                      "        \"iKey\": 1," +
                      "        \"fKey\": 1.1," +
                      "        \"sKey\": \"ss\"," +
                      "        \"bKey\": true" +
                      "        }]," +
                      "    \"arrEmpty\"  : []" +
                      "    }" +
                      "}";
        // ------ When
        JsonPath jsonPath = JsonPath.from(json);

        // ------ Then

        // test pre-#1746 behavior for objects
        assertThat(jsonPath.get("root.absentKey"), is(nullValue()));
        assertThat(jsonPath.get("root.nKey.absentKey"), is(nullValue()));

        // Test the #1746: change "IllegalArgumentException" to null when root.xKey has ifsb type in json
        // ifsb == int/float/string/boolean
        assertThat(jsonPath.get("root.iKey.absentKey"), is(nullValue()));
        assertThat(jsonPath.get("root.fKey.absentKey"), is(nullValue()));
        assertThat(jsonPath.get("root.sKey.absentKey"), is(nullValue()));
        assertThat(jsonPath.get("root.bKey.absentKey"), is(nullValue()));

        // Test the #1746, for items inside arrays:
        // Note that we cannot return [] here because of library design to use Groovy for processing
        // But 'null'  is better than pre-#1746 "IllegalArgumentException"
        assertThat(jsonPath.get("root.array.iKey.absentKey"), is(nullValue()));
        assertThat(jsonPath.get("root.array.fKey.absentKey"), is(nullValue()));
        assertThat(jsonPath.get("root.array.sKey.absentKey"), is(nullValue()));
        assertThat(jsonPath.get("root.array.bKey.absentKey"), is(nullValue()));

        // test pre-#1746 behavior for arrays:
        assertThat(jsonPath.get("root.array.absentKey"), allOf(hasSize(1), contains((Object)null)));  // [null]
        assertThat(jsonPath.get("root.array.nKey.absentKey"), is(empty()));  // []
        assertThat(jsonPath.get("root.arrEmpty.absentKey"), is(empty()));    // []

    }


    /** Test that fix #1746 did not break exception about parameters */
    @Test public void
    exception_should_be_thrown_for_undefined_script_parameters(){

        // Given
        JsonPath jsonPath = JsonPath.from("{}");

        Exception actualException = null;
        //When
        try{
            jsonPath/*.param("myParameter", "xx")*/.get("$==myParameter");
        }catch (IllegalArgumentException e){actualException = e;}

        //Then
        assertThat(actualException, is(notNullValue()));
        assertThat(actualException.getMessage(),
                   equalTo("The parameter \"myParameter\" was used but not defined. Define parameters using the " +
                           "JsonPath.param(...) function"));
    }

    @Test public void
    does_not_fail_on_primitive_string() {
        // When
        String json = "\"foo\"";

        // When
        JsonPath jsonPath = JsonPath.from(json);

        // Then
        assertThat(jsonPath.get("$"), is("foo"));
    }

    @Test public void
    does_not_fail_on_primitive_true() {
        // When
        String json = "true";

        // When
        JsonPath jsonPath = JsonPath.from(json);

        // Then
        assertThat(jsonPath.get("$"), is(true));
    }

    @Test public void
    does_not_fail_on_primitive_false() {
        // When
        String json = "false";

        // When
        JsonPath jsonPath = JsonPath.from(json);

        // Then
        assertThat(jsonPath.get("$"), is(false));
    }
    @Test public void
    does_not_fail_on_primitive_null() {
        // When
        String json = "null";

        // When
        JsonPath jsonPath = JsonPath.from(json);

        // Then
        assertThat(jsonPath.get("$"), nullValue());
    }
}
