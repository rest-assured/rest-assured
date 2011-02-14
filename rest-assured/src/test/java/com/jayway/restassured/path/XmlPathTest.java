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

package com.jayway.restassured.path;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.path.XmlPath.given;
import static com.jayway.restassured.path.XmlPath.with;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;

public class XmlPathTest {

    private static final String XML = "<shopping>\n" +
            "      <category type=\"groceries\">\n" +
            "        <item>\n" +
            "\t   <name>Chocolate</name>\n" +
            "           <price>10</price>\n" +
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

    @Test
    public void initializeUsingCtorAndGetList() throws Exception {
        final List<String> categories = new XmlPath(XML).get("shopping.category");
        assertThat(categories.size(), equalTo(3));
    }

    @Test
    public void initializeUsingGivenAndGetAttributes() throws Exception {
        final List<String> categories = given(XML).get("shopping.category.@type");
        assertThat(categories, hasItems("groceries", "supplies", "present"));
    }

    @Test
    public void initializeUsingWithAndGetList() throws Exception {
        final List<String> categories = with(XML).get("shopping.category");
        assertThat(categories.size(), equalTo(3));
    }

    @Test
    public void initializeUsingWithAndGetChildren() throws Exception {
        final List<String> categories = with(XML).get("shopping.category.item.name");
        assertThat(categories, hasItems("Chocolate", "Coffee", "Paper", "Pens", "Kathryn's Birthday"));
    }

    @Test
    public void returnItems() throws Exception {
        final List<String> categories = with(XML).get("shopping.category.item.children()");
        assertThat(categories, hasItems("Chocolate", "Coffee", "Paper", "Pens", "Kathryn's Birthday"));
    }

    @Test
    public void itemsWithPriceBetweenTenAndTwenty() throws Exception {
        final List<Map<String, List<String>>> itemsBetweenTenAndTwenty = with(XML).get("shopping.category.item.findAll { item -> def price = Float.parseFloat(item.price.text()); price >= 10 && price <= 20 }");
        assertThat(itemsBetweenTenAndTwenty.size(), equalTo(3));

        final Map<String, List<String>> category1 = itemsBetweenTenAndTwenty.get(0);
        final List<String> categoryChildren = category1.get("children");
        assertThat(categoryChildren, hasItems("name", "price"));
    }

    @Test
    public void multipleGetsWithOneInstanceOfXmlPath() throws Exception {
        final XmlPath xmlPath = new XmlPath(XML);
        assertThat(xmlPath.getInt("shopping.category.item.size()"), equalTo(5));
        assertThat(xmlPath.getList("shopping.category.item.children()", String.class), hasItem("Pens"));
    }

    @Test
    public void rootPathNotEndingWithDot() throws Exception {
        final XmlPath xmlPath = new XmlPath(XML).setRoot("shopping.category.item");
        assertThat(xmlPath.getInt("size()"), equalTo(5));
        assertThat(xmlPath.getList("children()", String.class), hasItem("Pens"));
    }

    @Test
    public void rootPathEndingWithDot() throws Exception {
        final XmlPath xmlPath = new XmlPath(XML).setRoot("shopping.category.item.");
        assertThat(xmlPath.getInt("size()"), equalTo(5));
        assertThat(xmlPath.getList("children()", String.class), hasItem("Pens"));
    }

    @Test
    public void convertsNonRootObjectGraphToJavaObjects() throws Exception {
        List<Map<String, Object>> objects = with(XML).get("shopping.category");
        assertThat(objects.size(), equalTo(3));
        assertThat(objects.toString(), equalTo("[{attributes={type=groceries}, @type=groceries, children=[item, {item={attributes={}, children=[name, {name={attributes={}, children=[], value=Chocolate}}, price, {price={attributes={}, children=[], value=10}}]}}, item, {item={attributes={}, children=[name, {name={attributes={}, children=[], value=Coffee}}, price, {price={attributes={}, children=[], value=20}}]}}]}, {attributes={type=supplies}, @type=supplies, children=[item, {item={attributes={}, children=[name, {name={attributes={}, children=[], value=Paper}}, price, {price={attributes={}, children=[], value=5}}]}}, item, {item={attributes={quantity=4}, @quantity=4, children=[name, {name={attributes={}, children=[], value=Pens}}, price, {price={attributes={}, children=[], value=15.5}}]}}]}, {attributes={type=present}, @type=present, children=[item, {item={attributes={when=Aug 10}, @when=Aug 10, children=[name, {name={attributes={}, children=[], value=Kathryn's Birthday}}, price, {price={attributes={}, children=[], value=200}}]}}]}]"));
    }

    @Test
    public void convertsRootObjectGraphToJavaObjects() throws Exception {
        Map<String, Object> objects = with(XML).get("shopping");
        assertThat(objects.toString(), equalTo("{attributes={}, children=[category, {category={attributes={type=groceries}, @type=groceries, children=[item, {item={attributes={}, children=[name, {name={attributes={}, children=[], value=Chocolate}}, price, {price={attributes={}, children=[], value=10}}]}}, item, {item={attributes={}, children=[name, {name={attributes={}, children=[], value=Coffee}}, price, {price={attributes={}, children=[], value=20}}]}}]}}, category, {category={attributes={type=supplies}, @type=supplies, children=[item, {item={attributes={}, children=[name, {name={attributes={}, children=[], value=Paper}}, price, {price={attributes={}, children=[], value=5}}]}}, item, {item={attributes={quantity=4}, @quantity=4, children=[name, {name={attributes={}, children=[], value=Pens}}, price, {price={attributes={}, children=[], value=15.5}}]}}]}}, category, {category={attributes={type=present}, @type=present, children=[item, {item={attributes={when=Aug 10}, @when=Aug 10, children=[name, {name={attributes={}, children=[], value=Kathryn's Birthday}}, price, {price={attributes={}, children=[], value=200}}]}}]}}]}"));
    }

    @Test
    public void firstCategoryAttributeFromJava() throws Exception {
        Map<String, String> objects = with(XML).get("shopping.category[0]");
        assertThat(objects.get("@type"), equalTo("groceries"));
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
}