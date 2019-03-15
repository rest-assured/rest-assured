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

import org.junit.Test;

import static io.restassured.path.xml.XmlPath.CompatibilityMode.HTML;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


public class HtmlPathTest {

    @Test public void
    can_extract_values_from_html() {
        // Given
        String html = "<html>\n" +
                "      <head>\n" +
                "        <title>my title</title>\n" +
                "      </head>\n" +
                "      <body>\n" +
                "        <p>paragraph 1</p>\n" +
                "        <p>paragraph 2</p>\n" +
                "      </body>\n" +
                "    </html>";

        // When
        XmlPath xmlPath = new XmlPath(HTML, html);

        // Then
        assertThat(xmlPath.getString("html.head.title"), equalTo("my title"));
    }
}
