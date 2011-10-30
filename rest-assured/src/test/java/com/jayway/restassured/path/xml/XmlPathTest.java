/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.path.xml;

import com.jayway.restassured.path.xml.element.Node;
import com.jayway.restassured.path.xml.element.NodeChildren;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.path.xml.XmlPath.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class XmlPathTest {

    private static final String XML = "<shopping>\n" +
            "      <category type=\"groceries\">\n" +
            "        <item>\n" +
            "\t   <name>Chocolate</name>\n" +
            "           <price>10</" +
            "price>\n" +
            "" +
            "   " +
            "\t</item>\n" +
            "        <item>\n" +
            "\t   <name>Coffee</name>\n" +
            "           <price>20</price>\n" +
            "\t</item>\n" +
            "      </category>\n" +
            "      <category type=\"supplies\">\n" +
            "        <item>\n" +
            "\t   <name>Paper</name>\n" +
            "           <price>5</price>\n" +
            "\t</item>\n" +
            "        <item quantity=\"4\">\n" +
            "           <name>Pens</name>\n" +
            "           <price>15.5</price>\n" +
            "\t</item>\n" +
            "      </category>\n" +
            "      <category type=\"present\">\n" +
            "        <item when=\"Aug 10\">\n" +
            "           <name>Kathryn's Birthday</name>\n" +
            "           <price>200</price>\n" +
            "        </item>\n" +
            "      </category>\n" +
            "</shopping>";

    private static final String GREETING = "<greeting><firstName>John</firstName>\n" +
            "      <lastName>Doe</lastName>\n" +
            "    </greeting>";

    private static final String RECORDS = "<records>\n" +
            "      <car name='HSV Maloo' make='Holden' year='2006'>\n" +
            "        <country>Australia</country>\n" +
            "        <record type='speed'>Pickup Truck with speed of 271kph</record>\n" +
            "      </car>\n" +
            "      <car name='P50' make='Peel' year='1962'>\n" +
            "        <country>Isle of Man</country>\n" +
            "        <record type='size'>Street-Legal Car at 99cm wide, 59kg</record>\n" +
            "      </car>\n" +
            "      <car name='Royale' make='Bugatti' year='1931'>\n" +
            "        <country>France</country>\n" +
            "        <record type='price'>Most Valuable Car at $15 million</record>\n" +
            "      </car>\n" +
            "</records>";

    private static final String ATTR_WITH_MINUS = "<something has-a-name=\"some\" />";

    private static final String XML_WITH_DOT_IN_NAME = "<something><com.mycompany.Filter>Hello</com.mycompany.Filter><some-value>Some value</some-value></something>";

    private static final String RSS = "<?xml version=\"1.0\"?>\n" +
            "<rss version=\"2.0\">\n" +
            "  <channel>\n" +
            "    <title>Some title</title>\n" +
            "    <link>http://www.google.com</link>\n" +
            "    <description>Description</description>\n" +
            "    <category domain=\"http://mycompany.com/category\">games</category>\n" +
            "    <item>\n" +
            "      <title>Item title</title>\n" +
            "      <link>http://www.somelink.org</link>\n" +
            "      <description>Some cool game</description>\n" +
            "      <enclosure length=\"58433\" type=\"image/jpeg\" url=\"https://mycompany.org/some.jpg\"/>\n" +
            "      <category domain=\"http://mycompany.com/rss/first\">First category</category>\n" +
            "      <category domain=\"http://mycompany.com/rss/second\">Second category</category>\n" +
            "      <pubDate>Sun, 04 Sep 2011 15:32:25 GMT</pubDate>\n" +
            "      <guid>1234</guid>\n" +
            "    </item>\n" +
            "  </channel>\n" +
            "</rss>";

    private static final String LIST_WITH_INTS = "<some>\n"+
            "  <thing id=\"1\">ikk</thing>\n"+
            "  <thing id=\"2\">ikk2</thing>\n"+
            "  <thing id=\"3\">ikk3</thing>\n"+
            "</some>";

    @Test
    public void initializeUsingCtorAndGetList() throws Exception {
        final NodeChildren categories = new XmlPath(XML).get("shopping.category");
        assertThat(categories.size(), equalTo(3));
    }

    @Test
    public void getNodeChildrenAsListWithTypeNodeReturnsAListOfNodes() throws Exception {
        final List<Node> categories = new XmlPath(XML).getList("shopping.category", Node.class);
        assertThat(categories.size(), equalTo(3));
    }

    @Test
    public void initializeUsingGivenAndGetAttributes() throws Exception {
        final List<String> categories = given(XML).get("shopping.category.@type");
        assertThat(categories, hasItems("groceries", "supplies", "present"));
    }

    @Test
    public void initializeUsingWithAndGetList() throws Exception {
        final NodeChildren categories = with(XML).get("shopping.category");
        assertThat(categories.size(), equalTo(3));
    }

    @Test
    public void initializeUsingWithAndGetChildren() throws Exception {
        final List<String> categories = with(XML).get("shopping.category.item.name.list()");
        assertThat(categories, hasItems("Chocolate", "Coffee", "Paper", "Pens", "Kathryn's Birthday"));
    }

    @Test
    public void returnItems() throws Exception {
        final List<String> categories = with(XML).get("shopping.category.item.children().list()");
        assertThat(categories, hasItems("Chocolate", "Coffee", "Paper", "Pens", "Kathryn's Birthday"));
    }

    @Test
    public void itemsWithPriceBetweenTenAndTwenty() throws Exception {
        final List<Node> itemsBetweenTenAndTwenty = with(XML).get("shopping.category.item.findAll { item -> def price = item.price.toFloat(); price >= 10 && price <= 20 }");
        assertThat(itemsBetweenTenAndTwenty.size(), equalTo(3));

        final Node category1 = itemsBetweenTenAndTwenty.get(0);
        final NodeChildren categoryChildren = category1.children();
        assertThat(categoryChildren, hasItems("Chocolate", "10"));

        for (Node item : categoryChildren.nodeIterable()) {
            assertThat(item.name(), anyOf(equalTo("name"), equalTo("price")));
        }
    }

    @Test
    public void multipleGetsWithOneInstanceOfXmlPath() throws Exception {
        final XmlPath xmlPath = new XmlPath(XML);
        assertThat(xmlPath.getInt("shopping.category.item.size()"), equalTo(5));
        assertThat(xmlPath.getList("shopping.category.item.children().list()", String.class), hasItem("Pens"));
    }

    @Test
    public void rootPathNotEndingWithDot() throws Exception {
        final XmlPath xmlPath = new XmlPath(XML).setRoot("shopping.category.item");
        assertThat(xmlPath.getInt("size()"), equalTo(5));
        assertThat(xmlPath.getList("children().list()", String.class), hasItem("Pens"));
    }

    @Test
    public void rootPathEndingWithDot() throws Exception {
        final XmlPath xmlPath = new XmlPath(XML).setRoot("shopping.category.item.");
        assertThat(xmlPath.getInt("size()"), equalTo(5));
        assertThat(xmlPath.getList("children().list()", String.class), hasItem("Pens"));
    }

    @Test
    public void convertsNonRootObjectGraphToJavaObjects() throws Exception {
        NodeChildren categories = with(XML).get("shopping.category");
        assertThat(categories.size(), equalTo(3));
        assertThat(categories.toString(), equalTo("Chocolate10Coffee20Paper5Pens15.5Kathryn's Birthday200"));
    }

    @Test
    public void convertsRootObjectGraphToJavaObjects() throws Exception {
        Node objects = with(XML).get("shopping");
        assertThat(objects.toString(), equalTo("Chocolate10Coffee20Paper5Pens15.5Kathryn's Birthday200"));
    }

    @Test
    public void firstCategoryAttributeFromJava() throws Exception {
        Node node = with(XML).get("shopping.category[0]");
        assertThat(node.getAttribute("@type"), equalTo("groceries"));
        assertThat(node.getAttribute("type"), equalTo("groceries"));
        assertThat((String) node.get("@type"), equalTo("groceries"));
    }

    @Test
    public void gettingChildrenFromJava() throws Exception {
        Node category = with(XML).get("shopping.category[0]");
        final NodeChildren categoryChildren = category.children();
        assertThat(categoryChildren.size(), equalTo(2));
        for (Node item : categoryChildren.nodeIterable()) {
            assertThat(item.children().size(), equalTo(2));
            final Node name = item.get("name");
            final Node price = item.get("price");
            assertThat(name.value(), anyOf(equalTo("Chocolate"), equalTo("Coffee")));
            assertThat(price.value(), anyOf(equalTo("10"), equalTo("20")));
        }
    }

    @Test
    public void getFirstItemName() throws Exception {
        final String name = with(XML).get("shopping.category.item[0].name");
        assertThat(name, equalTo("Chocolate"));
        assertThat(with(XML).getString("shopping.category.item[0].name"), equalTo("Chocolate"));
    }

    @Test
    public void getSingleAttributes() throws Exception {
        final Map<String, String> categoryAttributes = with(XML).get("shopping.category[0].attributes()");
        assertThat(categoryAttributes.size(), equalTo(1));
        assertThat(categoryAttributes.get("type"), equalTo("groceries"));
    }

    @Test
    public void gettingLeafReturnsValueInsteadOfNode() throws Exception {
        String firstName = with(GREETING).get("greeting.firstName");
        String lastName = with(GREETING).get("greeting.lastName");

        assertThat(firstName, equalTo("John"));
        assertThat(lastName, equalTo("Doe"));
    }

    @Test
    public void getAllItemNames() throws Exception {
        final List<String> items = with(XML).get("shopping.depthFirst().grep { it.name() == 'item' }.name");

        assertThat(items, hasItems("Chocolate", "Coffee", "Paper", "Pens", "Kathryn's Birthday"));
    }

    @Test
    public void getEntireObjectGraph() throws Exception {
        final Node node = with(XML).get();

        assertThat(node.name(), is("shopping"));
    }

    @Test
    public void getLocationOfRecords() throws Exception {
        final List<String> list = from(RECORDS).getList("records.car.country.list()", String.class);

        assertThat(list, hasItems("Australia", "Isle of Man", "France"));
    }

    @Test
    public void getFirstTwoYearsOfRecords() throws Exception {
        final List<String> list = from(RECORDS).getList("records.car[0..1].@year", String.class);

        assertThat(list, hasItems("2006", "1962"));
    }

    @Test
    public void getFirstTwoYearsOfRecordsUsingEscapedAttributeGetter() throws Exception {
        final List<String> list = from(RECORDS).getList("records.car[0..1].'@year'", String.class);

        assertThat(list, hasItems("2006", "1962"));
    }

    @Test
    public void getNameOfLastCountry() throws Exception {
        final String country = from(RECORDS).getString("records.car[-1].country");

        assertThat(country, equalTo("France"));
    }

    @Test
    public void getNameOfFirstCar() throws Exception {
        final String name = from(RECORDS).getString("records.car[0].@name");

        assertThat(name, equalTo("HSV Maloo"));
    }

    @Test
    public void getIntParsesAStringResultToInt() throws Exception {
        final int price = from(XML).getInt("shopping.category[0].item[0].price");

        assertThat(price, equalTo(10));
    }

    @Test
    public void depthFirstSearchingUsingDoubleStarNotation() throws Exception {
        final int chocolatePrice = from(XML).getInt("shopping.'**'.find { it.name == 'Chocolate' }.price");

        assertThat(chocolatePrice, equalTo(10));
    }

    @Test
    public void depthFirstSearchingUsingUnEscapedDoubleStarNotation() throws Exception {
        final int chocolatePrice = from(XML).getInt("shopping.**.find { it.name == 'Chocolate' }.price");

        assertThat(chocolatePrice, equalTo(10));
    }

    @Test
    public void getListReturnsListWhenNodeChildrenFound() {
        final List<String> groceries = from(XML).getList("shopping.category[0].item.name");
        assertThat(groceries, hasItems("Chocolate", "Coffee"));
    }

    @Test
    public void getListReturnsListWhenListFound() {
        final List<String> groceries = from(XML).getList("shopping.category[0].item.name.list()");

        assertThat(groceries, hasItems("Chocolate", "Coffee"));
    }

    @Test
    public void getListWhenUsingExplicitTypeConvertsTheListMembersToThatType() {
        final List<Float> groceries = from(XML).getList("shopping.category.item.price", Float.class);

        assertThat(groceries, hasItems(10.0f, 20.0f, 5.0f, 15.5f, 200.0f));
    }

    @Test
    public void getListWhenNotUsingExplicitTypeDoesntConvertTheListMembersToAnyType() {
        final List<String> groceries = from(XML).getList("shopping.category.item.price");

        assertThat(groceries, hasItems("10", "20", "5", "15.5", "200"));
    }

    @Test
    public void getListAutomaticallyTransformsSingleObjectResultsToAList() {
        final List<String> groceries = from(XML).getList("shopping.category.item.price[0]");

        assertThat(groceries, hasItems("10"));
    }

    @Test
    public void getAutomaticallyTransformsSingleObjectResultsToAListWhenSpecifiedInPath() {
        final List<String> groceries = from(XML).get("shopping.category.item.price[0].list()");

        assertThat(groceries, hasItems("10"));
    }

    @Test
    public void rootDepthFirstSearchingWhenUsingDoubleStarNotation() throws Exception {
        final List<String> groceries = from(XML).getList("**.find { it.@type == 'groceries' }.item.name");

        assertThat(groceries, hasItems("Chocolate", "Coffee"));
    }

    @Test
    public void rootDepthFirstSearchingWhenUsingEscapedDoubleStarNotation() throws Exception {
        final List<String> groceries = from(XML).getList("'**'.find { it.@type == 'groceries' }.item.name");

        assertThat(groceries, hasItems("Chocolate", "Coffee"));
    }

    @Test
    public void rootDepthFirstSearchingWhenUsingDoubleStarNotationWhenPathStartsWithDot() throws Exception {
        final List<String> groceries = from(XML).getList(".**.find { it.@type == 'groceries' }.item.name");

        assertThat(groceries, hasItems("Chocolate", "Coffee"));
    }

    @Test
    public void rootDepthFirstMethodSearching() throws Exception {
        final List<String> groceries = from(XML).getList("depthFirst().find { it.@type == 'groceries' }.item.name");

        assertThat(groceries, hasItems("Chocolate", "Coffee"));
    }

    @Test
    public void canParsePathWithDoubleEscapeChars() throws Exception {
        final String name = from(ATTR_WITH_MINUS).getString("something.@has-a-name");

        assertThat(name, equalTo("some"));
    }

    @Test
    public void canParseTagsWithDotIfUsingEscaping() throws Exception {
        final String message = from(XML_WITH_DOT_IN_NAME).get("something.'com.mycompany.Filter'");

        assertThat(message, equalTo("Hello"));
    }

    @Test
    public void canParseTagsWithEscapedMinus() throws Exception {
        final String message = from(XML_WITH_DOT_IN_NAME).getString("something.'some-value'");

        assertThat(message, equalTo("Some value"));
    }

    @Test
    public void canParseClosuresWithEscapedDotsInEqualExpression() throws Exception {
        final String firstCategory = from(RSS).get("rss.**.find { it.@domain == 'http://mycompany.com/rss/first' }");

        assertThat(firstCategory, equalTo("First category"));
    }

    @Test
    public void convertsListMembersToExplicitType() throws Exception {
        List<Integer> ids = from(LIST_WITH_INTS).getList("some.thing.@id", int.class);

        assertThat(ids, hasItems(1,2,3));
    }
}