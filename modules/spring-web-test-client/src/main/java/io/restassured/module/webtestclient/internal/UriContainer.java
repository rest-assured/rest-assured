package io.restassured.module.webtestclient.internal;

import org.springframework.lang.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

public class UriContainer {
    private final String uri;
    private final Map<String, Object> uriVariables;

    private UriContainer(Builder builder) {
        this.uri = builder.uri;
        this.uriVariables = builder.uriVariables;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, Object> getUriVariables() {
        return uriVariables;
    }

    public static Builder newBuilder(@NonNull String uri) {
        notNull(uri, "uri");
        return new Builder(uri);
    }

    public static final class Builder {
        private String uri;
        private Map<String, Object> uriVariables = new LinkedHashMap<>();

        private Builder(@NonNull String uri) {
            notNull(uri, "uri");
            this.uri = uri;
        }

        public Builder uri(@NonNull String uri) {
            notNull(uri, "uri");
            this.uri = uri;
            return this;
        }

        public Builder uriVariables(@NonNull Map<String, Object> uriVariables) {
            notNull(uriVariables, "uriVariables");
            this.uriVariables = new LinkedHashMap<>(uriVariables);
            return this;
        }

        public UriContainer build() {
            return new UriContainer(this);
        }
    }
}
