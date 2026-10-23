/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.internal.path.xml;

import io.restassured.common.config.NumberReturnType;
import io.restassured.internal.common.spi.path.DocumentType;
import io.restassured.internal.common.spi.path.PathInput;
import io.restassured.internal.common.spi.path.PathResult;
import io.restassured.internal.common.spi.path.PathSettings;
import io.restassured.path.xml.exception.XmlPathException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GPathXmlProviderTest {

    private final GPathXmlProvider provider = new GPathXmlProvider();

    private static PathSettings settings(boolean namespaceAware, Map<String, String> declaredNamespaces) {
        return new PathSettings(NumberReturnType.FLOAT_AND_DOUBLE, 1000, false, namespaceAware, false, Collections.emptyMap(), Collections.emptyMap(), declaredNamespaces);
    }

    private Object evaluate(PathInput input, DocumentType type, PathSettings settings, String path) throws Exception {
        PathResult result = provider.evaluate(provider.parse(input, type, settings), type, path);
        assertThat(result.kind()).isEqualTo(PathResult.Kind.VALUE);
        return result.value();
    }

    @Test
    void describes_itself() {
        assertThat(provider.artifactId()).isEqualTo("xml-path");
        assertThat(provider.engineName()).isEqualTo("GPATH");
        assertThat(provider.documentTypes()).containsExactlyInAnyOrder(DocumentType.XML, DocumentType.HTML);
        assertThat(provider.description(DocumentType.XML)).isEqualTo("XML path");
    }

    @Test
    void evaluates_paths_on_text_on_a_stream_and_on_a_stream_in_a_given_charset() throws Exception {
        PathSettings settings = settings(true, Collections.emptyMap());
        String xml = "<a><b>åäö</b></a>";

        assertThat(evaluate(PathInput.ofText(xml), DocumentType.XML, settings, "a.b")).isEqualTo("åäö");
        assertThat(evaluate(PathInput.ofStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null), DocumentType.XML, settings, "a.b")).isEqualTo("åäö");
        assertThat(evaluate(PathInput.ofStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.ISO_8859_1)), "ISO-8859-1"), DocumentType.XML, settings, "a.b")).isEqualTo("åäö");
    }

    @Test
    void parses_html_leniently() throws Exception {
        assertThat(evaluate(PathInput.ofText("<html><body><p>x</body></html>"), DocumentType.HTML, settings(true, Collections.emptyMap()), "html.body.p")).isEqualTo("x");
    }

    @Test
    void declares_namespaces_only_when_namespace_aware() throws Exception {
        String xml = "<a:root xmlns:a=\"urn:a\"><a:child>x</a:child></a:root>";
        Map<String, String> namespaces = Collections.singletonMap("b", "urn:a");

        assertThat(evaluate(PathInput.ofText(xml), DocumentType.XML, settings(true, namespaces), "root.'b:child'")).isEqualTo("x");
        assertThat(evaluate(PathInput.ofText(xml), DocumentType.XML, settings(false, namespaces), "'a:root'.'a:child'")).isEqualTo("x");
    }

    @Test
    void parse_failure_is_an_xml_path_exception() {
        Exception cause = new IllegalArgumentException("x");

        assertThat(provider.parseFailure(DocumentType.XML, cause))
                .isExactlyInstanceOf(XmlPathException.class).hasMessage("Failed to parse the XML document").hasCause(cause);
    }

    @Test
    void joins_root_paths_like_gpath() {
        assertThat(provider.joinRootPath("a.b", "c")).isEqualTo("a.b.c");
        assertThat(provider.joinRootPath("a", "[0]")).isEqualTo("a[0]");
        assertThat(provider.joinRootPath(null, "c")).isEqualTo("c");
    }
}
