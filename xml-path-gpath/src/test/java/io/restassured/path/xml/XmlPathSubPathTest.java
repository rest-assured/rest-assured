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

package io.restassured.path.xml;

import io.restassured.path.xml.element.Node;
import io.restassured.path.xml.element.NodeChildren;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static io.restassured.path.xml.XmlPath.with;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

public class XmlPathSubPathTest {

    @Language("XML")
    private static final String XML = """
            <shopping>
                  <category type="groceries">
                    <item>
               <name>Chocolate</name>
                       <price>10</price>
            </item>
                    <item>
               <name>Coffee</name>
                       <price>20</price>
            </item>
                  </category>
                  <category type="supplies">
                    <item>
               <name>Paper</name>
                       <price>5</price>
            </item>
                    <item quantity="4">
                       <name>Pens</name>
                       <price>15.5</price>
            </item>
                  </category>
                  <category type="present">
                    <item when="Aug 10">
                       <name>Kathryn's Birthday</name>
                       <price>200</price>
                    </item>
                  </category>
            </shopping>""";

    @Test public void
    subpath_works_for_lists() {
        Node category = with(XML).get("shopping");
        final NodeChildren names = category.getPath("category[0].item.name");

        assertThat(names, hasItems("Chocolate", "Coffee"));
    }

    @Test public void
    subpath_with_explicit_type() {
        Node category = with(XML).get("shopping");
        final float firstPrice = category.getPath("category[0].item.price[0]", float.class);

        assertThat(firstPrice, is(10f));
    }

    @Test public void
    error_messages_on_invalid_subpath_looks_ok() {
        Node category = with(XML).get("shopping");

        assertThatThrownBy(() -> category.getPath("category[0].item.price..[0]", float.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.format("Invalid path:%n" +
                        "Unexpected input: '[0].item.price.[' @ line 1, column 49.%n" +
                        "   category[0].item.price.[0]%n" +
                        "                          ^%n" +
                        "%n" +
                        "1 error"));
    }
}