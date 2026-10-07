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
 * The version is selected like this:
 * </p>
 * <ol>
 * <li>A <code>$schema</code> declaring draft-03 or draft-04 is validated by the
 * <a href="https://github.com/java-json-tools/json-schema-validator">java-json-tools json-schema-validator</a>, exactly as in previous
 * versions of REST Assured.</li>
 * <li>A <code>$schema</code> declaring draft-06, draft-07, 2019-09 or 2020-12 is validated according to that draft by the
 * <a href="https://github.com/networknt/json-schema-validator">networknt json-schema-validator</a>.</li>
 * <li>Schemas without <code>$schema</code> (or with an unknown <code>$schema</code>) are validated according to the version configured by
 * {@link JsonSchemaValidatorSettings#schemaVersion(JsonSchemaVersion)} or {@link JsonSchemaValidator#using(JsonSchemaVersion)}.
 * If no version is configured they're validated as draft-04 by the java-json-tools validator, as before.</li>
 * </ol>
 * <p>
 * So a configured version never overrides a <code>$schema</code> declared by the schema. It's useful when your schemas don't declare
 * <code>$schema</code> but use newer keywords such as <code>const</code> or <code>if</code>/<code>then</code>/<code>else</code>:
 * </p>
 * <pre>
 * get("/products").then().body(matchesJsonSchemaInClasspath("products-schema.json").using(JsonSchemaVersion.DRAFT_2020_12));
 * </pre>
 * <p>
 * Things to be aware of for draft-06, draft-07, 2019-09 and 2020-12 (validated by networknt):
 * </p>
 * <ul>
 * <li>The {@link com.github.fge.jsonschema.main.JsonSchemaFactory} and {@link JsonSchemaValidatorSettings#checkedValidation(boolean) checked validation}
 * settings only apply to the java-json-tools validator. They can't be honored for these drafts since that validator doesn't understand them
 * (newer keywords would silently be ignored).</li>
 * <li>As mandated by the 2019-09 and 2020-12 specifications, <code>format</code> is only an annotation for these drafts, i.e. it's not validated.</li>
 * <li>Schemas loaded from a remote URL or URI (for example <code>http</code>) are only inspected for <code>$schema</code> if
 * {@link JsonSchemaValidatorSettings#parseUriAndUrlsAsJsonNode(boolean)} is <code>true</code> or a version is configured, so that the schema
 * isn't downloaded twice. Classpath and file schemas are always inspected.</li>
 * <li>Relative <code>$ref</code>'s are resolved against the location of the schema, so they require the schema to be loaded from the classpath,
 * a File, URL or URI (not from a String, InputStream or Reader).</li>
 * </ul>
 */
public enum JsonSchemaVersion {
    /**
     * JSON Schema draft-04, validated by the java-json-tools validator (using the {@link com.github.fge.jsonschema.main.JsonSchemaFactory}
     * configured in {@link JsonSchemaValidatorSettings}). This is the default for schemas without <code>$schema</code>.
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
     * @return <code>true</code> if the value identifies draft-03 or draft-04, which are validated by the java-json-tools validator.
     */
    static boolean isLegacyMetaSchemaUri(String schemaKeywordValue) {
        if (schemaKeywordValue == null) {
            return false;
        }
        String normalized = normalize(schemaKeywordValue);
        return normalized.equals(normalize(DRAFT_4.metaSchemaUri)) || normalized.equals(normalize(DRAFT_3_META_SCHEMA_URI));
    }

    boolean isLegacy() {
        return this == DRAFT_4;
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
