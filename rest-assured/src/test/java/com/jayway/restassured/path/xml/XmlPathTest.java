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

import static com.jayway.restassured.matcher.RestAssuredMatchers.matchesXsd;
import static com.jayway.restassured.path.xml.XmlPath.given;
import static com.jayway.restassured.path.xml.XmlPath.with;
import static org.hamcrest.CoreMatchers.equalTo;
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

    @Test
    public void initializeUsingCtorAndGetList() throws Exception {
        final NodeChildren categories = new XmlPath(XML).get("shopping.category");
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
}