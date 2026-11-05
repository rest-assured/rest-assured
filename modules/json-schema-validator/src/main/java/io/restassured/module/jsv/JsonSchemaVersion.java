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

package io.restassured.module.jsv;

import java.util.Locale;

/**
 * The JSON Schema specification version (draft) that the {@link JsonSchemaValidator} should validate against.
 * <p>
 * The version is taken from the <code>$schema</code> keyword of the schema. Schemas without <code>$schema</code> (or with an unknown
 * <code>$schema</code>) are validated according to the version configured by {@link JsonSchemaValidatorSettings#schemaVersion(JsonSchemaVersion)}
 * or {@link JsonSchemaValidator#using(JsonSchemaVersion)}, and as draft-04 if none is configured. A configured version never overrides
 * a <code>$schema</code> declared by the schema. It's useful when your schemas don't declare <code>$schema</code> but use newer keywords
 * such as <code>const</code> or <code>if</code>/<code>then</code>/<code>else</code>:
 * </p>
 * <pre>
 * get("/products").then().body(matchesJsonSchemaInClasspath("products-schema.json").using(JsonSchemaVersion.DRAFT_2020_12));
 * </pre>
 * <p>
 * Which validator is used:
 * </p>
 * <ul>
 * <li>Draft-06, draft-07, 2019-09 and 2020-12 are validated by the <a href="https://github.com/networknt/json-schema-validator">networknt
 * json-schema-validator</a>.</li>
 * <li>Draft-04 is validated by the <a href="https://github.com/java-json-tools/json-schema-validator">java-json-tools json-schema-validator</a>
 * if it's on the classpath, exactly as in REST Assured 6, and otherwise by the networknt json-schema-validator.</li>
 * <li>Draft-03 requires the java-json-tools json-schema-validator. Add <code>com.github.java-json-tools:json-schema-validator:2.2.14</code>
 * to validate it.</li>
 * </ul>
 * <p>
 * Things to be aware of:
 * </p>
 * <ul>
 * <li>The {@link com.github.fge.jsonschema.main.JsonSchemaFactory} and {@link JsonSchemaValidatorSettings#checkedValidation(boolean) checked validation}
 * settings only apply to the java-json-tools validator.</li>
 * <li>As mandated by the 2019-09 and 2020-12 specifications, <code>format</code> is only an annotation for these drafts, i.e. it's not validated.</li>
 * <li>Schemas loaded from a remote URL or URI (for example <code>http</code>) are only inspected for <code>$schema</code> when they're
 * parsed anyway, so that the schema isn't downloaded twice: when java-json-tools isn't on the classpath,
 * {@link JsonSchemaValidatorSettings#parseUriAndUrlsAsJsonNode(boolean)} is <code>true</code> or a version other than draft-04 is configured.
 * Classpath and file schemas are always inspected.</li>
 * <li>Relative <code>$ref</code>'s are resolved against the location of the schema, so they require the schema to be loaded from the classpath,
 * a File, URL or URI (not from a String, InputStream or Reader).</li>
 * </ul>
 */
public enum JsonSchemaVersion {
    /**
     * JSON Schema draft-04, the default for schemas without <code>$schema</code>.
     */
    DRAFT_4("http://json-schema.org/draft-04/schema"),
    /**
     * JSON Schema draft-06.
     */
    DRAFT_6("http://json-schema.org/draft-06/schema"),
    /**
     * JSON Schema draft-07.
     */
    DRAFT_7("http://json-schema.org/draft-07/schema"),
    /**
     * JSON Schema 2019-09.
     */
    DRAFT_2019_09("https://json-schema.org/draft/2019-09/schema"),
    /**
     * JSON Schema 2020-12.
     */
    DRAFT_2020_12("https://json-schema.org/draft/2020-12/schema");

    private static final String DRAFT_3_META_SCHEMA_URI = "http://json-schema.org/draft-03/schema";

    private final String metaSchemaUri;

    JsonSchemaVersion(String metaSchemaUri) {
        this.metaSchemaUri = metaSchemaUri;
    }

    /**
     * @return The URI of the meta-schema of this version, as used in the <code>$schema</code> keyword.
     */
    public String getMetaSchemaUri() {
        return metaSchemaUri;
    }

    /**
     * Find the JSON Schema version identified by a <code>$schema</code> value. The comparison ignores the URI scheme
     * (<code>http</code>/<code>https</code>), case and a trailing empty fragment (<code>#</code>).
     *
     * @param schemaKeywordValue The value of the <code>$schema</code> keyword.
     * @return The matching version or <code>null</code> if the value doesn't identify a known version.
     */
    public static JsonSchemaVersion fromMetaSchemaUri(String schemaKeywordValue) {
        if (schemaKeywordValue == null) {
            return null;
        }
        String normalized = normalize(schemaKeywordValue);
        for (JsonSchemaVersion version : values()) {
            if (normalize(version.metaSchemaUri).equals(normalized)) {
                return version;
            }
        }
        return null;
    }

    /**
     * @param schemaKeywordValue The value of the <code>$schema</code> keyword.
     * @return <code>true</code> if the value identifies draft-03, which only the java-json-tools validator supports.
     */
    static boolean isDraft3MetaSchemaUri(String schemaKeywordValue) {
        return schemaKeywordValue != null && normalize(schemaKeywordValue).equals(normalize(DRAFT_3_META_SCHEMA_URI));
    }

    private static String normalize(String uri) {
        String normalized = uri.trim().toLowerCase(Locale.ROOT);
        if (normalized.endsWith("#")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int schemeSeparator = normalized.indexOf("://");
        return schemeSeparator == -1 ? normalized : normalized.substring(schemeSeparator + 3);
    }
}
