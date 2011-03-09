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

package com.jayway.restassured

class TestGroovy {

  public static void main(String...string ) {
    def input = '''
<shopping>
    <johan>hej</johan>
    <category type="groceries">
        <item>Chocolate</item>
        <item>Coffee</item>
    </category>
    <category type="supplies">
        <item>Paper</item>
        <item quantity="4">Pens</item>
    </category>
    <category type="present">
        <item when="Aug 10">Kathryn's Birthday</item>
    </category>
</shopping>
'''

    def textHtml = '''
<html>
      <head>
        <title>my title</title>
      </head>
      <body>
        <p>paragraph 1</p>
        <p>paragraph 2</p>
      </body>
    </html>'''

private final String XML = "<shopping>\n" +
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
            "           <price>15</price>\n" +
            "\t</item>\n" +
            "      </category>\n" +
            "      <category type=\"present\">\n" +
            "        <item when=\"Aug 10\">\n" +
            "           <name>Kathryn's Birthday</name>\n" +
            "           <price>200</price>\n" +
            "        </item>\n" +
            "      </category>\n" +
            "</shopping>";


    def greeting = '''<greeting><firstName>John</firstName>
      <lastName>Doe</lastName>
    </greeting>'''

    def anotherGreeting = '''<greeting>
      <name>
        <firstName>firstName</firstName>
        <lastName>lastName</lastName>
      </name>
    </greeting>'''

    def json = "{ \"store\": {\n" +
            "    \"book\": [ \n" +
            "      { \"category\": \"reference\",\n" +
            "        \"author\": \"Nigel Rees\",\n" +
            "        \"title\": \"Sayings of the Century\",\n" +
            "        \"price\": 8.95\n" +
            "      },\n" +
            "      { \"category\": \"fiction\",\n" +
            "        \"author\": \"Evelyn Waugh\",\n" +
            "        \"title\": \"Sword of Honour\",\n" +
            "        \"price\": 12.99\n" +
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
            "    }\n" +
            "  }\n" +
            "}";

    def root = new XmlSlurper().parseText(XML)
//    def root = new JsonSlurper().parseText(json);

    System.out.println(root.category[0].getClass())

//    System.out.println(root.category[0].item[0].name);
  }
}