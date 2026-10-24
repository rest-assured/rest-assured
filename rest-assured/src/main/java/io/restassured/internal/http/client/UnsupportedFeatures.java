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

package io.restassured.internal.http.client;

import io.restassured.common.spi.http.ClientCustomizer;
import io.restassured.common.spi.http.HttpClientProvider;
import io.restassured.common.spi.http.UnsupportedFeatureException;
import io.restassured.internal.http.client.HttpClientProviders.Selection;

import java.util.List;

import static io.restassured.internal.http.client.HttpClientProviders.startOfSentence;

/**
 * The messages for what the chosen HTTP client can't do, which say which client it is, how it was chosen and what to do.
 */
public final class UnsupportedFeatures {

    private UnsupportedFeatures() {
    }

    /**
     * @return The exception of the client with a message that says which client it is, how it was chosen and what to do
     */
    public static UnsupportedOperationException translate(Selection selection, UnsupportedFeatureException e) {
        String fix;
        switch (selection.how()) {
            // Not reached until the JDK client becomes the default module, when HttpClient 4 is removed
            case DEFAULT:
                fix = "Add io.rest-assured:http-client-apache5 to your test dependencies and REST Assured uses it";
                break;
            case CLASSPATH:
                fix = "Choose another client with httpClientConfig().client(..)";
                break;
            default:
                fix = "Choose another client";
        }
        return new UnsupportedOperationException(clientAndHow(selection) + " can't " + e.feature() + ". " + fix
                + (e.workaround() == null ? "." : ", or " + e.workaround() + "."), e);
    }

    /**
     * For what REST Assured only does with Apache HttpClient 4 so far.
     *
     * @param feature    What the client can't do, after "can't", with "yet" if REST Assured will do it later
     * @param workaround What to do instead with the chosen client, after "or"
     * @return The exception with a message that says which client it is, how it was chosen and how to use HttpClient 4
     */
    public static UnsupportedOperationException httpClient4Only(Selection selection, String feature, String workaround) {
        String useHttpClient4;
        switch (selection.how()) {
            case CONFIG:
                useHttpClient4 = "Remove httpClientConfig().client(..) to use Apache HttpClient 4";
                break;
            case SYSTEM_PROPERTY:
                useHttpClient4 = "Remove the system property " + HttpClientProviders.SYSTEM_PROPERTY + " to use Apache HttpClient 4";
                break;
            case CLASSPATH:
                useHttpClient4 = "Remove " + selection.provider().artifact() + " from the classpath to use Apache HttpClient 4";
                break;
            default:
                // Not reached until the JDK client becomes the default module, when HttpClient 4 is removed
                useHttpClient4 = "Choose another client with httpClientConfig().client(..)";
        }
        return new UnsupportedOperationException(clientAndHow(selection) + " can't " + feature + ". " + useHttpClient4 + ", or " + workaround + ".");
    }

    private static String clientAndHow(Selection selection) {
        HttpClientProvider provider = selection.provider();
        String artifactId = provider.artifact().substring(provider.artifact().indexOf(':') + 1);
        String how;
        switch (selection.how()) {
            case DEFAULT:
                how = "the default module, " + artifactId;
                break;
            case CLASSPATH:
                how = "found on the classpath, " + artifactId;
                break;
            default:
                how = "chosen with " + selection.chosenBy();
        }
        return startOfSentence(provider.displayName()) + " (" + how + ")";
    }

    /**
     * @throws UnsupportedOperationException If a customizer is for the builder of another client than the chosen one
     */
    public static void verifyCustomizers(Selection selection, List<ClientCustomizer> customizers) {
        if (customizers.isEmpty()) {
            return;
        }
        Class<?> builderType = selection.isHttpClient4() ? null : selection.provider().builderType();
        for (ClientCustomizer customizer : customizers) {
            if (builderType == null || !customizer.builderType().isAssignableFrom(builderType)) {
                String message = "A customizer for " + customizer.builderType().getName() + " is configured, but REST Assured uses " + selection.displayName();
                if (selection.isHttpClient4()) {
                    message += ", which you customize with httpClientConfig().httpClientFactory(..)";
                } else if (builderType == null) {
                    message += ", which can't be customized";
                }
                throw new UnsupportedOperationException(message + ".");
            }
        }
    }
}
