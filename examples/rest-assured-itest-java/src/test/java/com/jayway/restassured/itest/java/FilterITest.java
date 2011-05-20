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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.internal.filter.FormAuthFilter;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.response.Response;
import org.junit.Ignore;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.xml.XmlPath.with;
import static org.hamcrest.Matchers.equalTo;

public class FilterITest extends WithJetty {

    @Test
    public void filterWorks() throws Exception {
        final FormAuthFilter filter = new FormAuthFilter();
        filter.setUserName("John");
        filter.setPassword("Doe");

        given().
                filter(filter).
        expect().
                statusCode(200).
                body(equalTo("OK")).
        when().
                get("/formAuth");
    }

    @Test
    public void test() throws Exception {
        String xml = "<html>\n" +
                "      <head>\n" +
                "        <title>Login</title>\n" +
                "      </head>\n" +
                "\n" +
                "      <body>\n" +
                "        <form action=\"j_spring_security_check\" method=\"POST\">\n" +
                "          <table>\n" +
                "            <tr><td>User:</td><td><input type='text' name='j_username'/></td></tr>\n" +
                "            <tr><td>Password:</td><td><input type='password' name='j_password' /></td></tr>\n" +
                "              <tr><td colspan='2'><input name=\"submit\" type=\"submit\"/></td></tr>\n" +
                "           </table>\n" +
                "            </form>\n" +
                "          </body>\n" +
                "     </html>";
        final Object formAction = with(xml).getString("html.depthFirst().grep { it.name() == 'input' && it.@type == 'text' }.collect { it.@name }");
        with(xml).getString("html.depthFirst().grep { it.name() == 'input' && it.@type == 'text' }.collect { it.@name }");
        System.out.println(formAction);
    }
}
