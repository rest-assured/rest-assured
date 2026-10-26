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

import io.restassured.RestAssured;
import io.restassured.authentication.AuthenticationContext;
import io.restassured.authentication.CertAuthScheme;
import io.restassured.authentication.CertificateAuthSettings;
import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.HostnameVerification;
import io.restassured.common.spi.http.HttpVersion;
import io.restassured.common.spi.http.KeyMaterial;
import io.restassured.common.spi.http.NtlmCredentials;
import io.restassured.common.spi.http.ProxySettings;
import io.restassured.common.spi.http.RequestOptions;
import io.restassured.common.spi.http.TlsSettings;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.SSLConfig;
import io.restassured.internal.http.RelaxedCertAuthScheme;
import io.restassured.specification.ProxySpecification;
import org.codehaus.groovy.runtime.GStringImpl;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static io.restassured.authentication.CertificateAuthSettings.certAuthSettings;
import static io.restassured.config.SSLConfig.sslConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

class ClientSettingsFactoryTest {
    private static final String CLIENT_KEY_STORE = "client_certificate_auth.p12";
    private static final String SERVER_KEY_STORE = "self_signed_localhost.p12";
    private static final String PASSWORD = "changeit";
    private static final HostnameVerifier CUSTOM_VERIFIER = (host, session) -> true;

    // SSLConfig

    @Test
    void the_ssl_config_is_left_out_unless_the_user_configured_it() {
        assertThat(clientSettings(sslConfig()).tls()).isNull();
        assertThat(clientSettings(sslConfig().strictHostnames()).tls()).isEqualTo(ClientSettingsFactory.tlsSettings(sslConfig()));
    }

    @Test
    void the_same_ssl_config_gives_equal_settings() {
        assertThat(clientSettings(sslConfig().relaxedHTTPSValidation())).isEqualTo(clientSettings(sslConfig().relaxedHTTPSValidation()));
        assertThat(clientSettings(sslConfig().relaxedHTTPSValidation()).hashCode()).isEqualTo(clientSettings(sslConfig().relaxedHTTPSValidation()).hashCode());
        assertThat(clientSettings(sslConfig().keyStore(CLIENT_KEY_STORE, PASSWORD).trustStore(SERVER_KEY_STORE, PASSWORD)))
                .isEqualTo(clientSettings(sslConfig().keyStore(CLIENT_KEY_STORE, PASSWORD).trustStore(SERVER_KEY_STORE, PASSWORD)));
    }

    @Test
    void every_ssl_config_value_that_the_settings_hold_changes_them() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Map<String, UnaryOperator<SSLConfig>> changes = Map.ofEntries(
                Map.entry("key store path", c -> c.keyStore(CLIENT_KEY_STORE, PASSWORD)),
                Map.entry("key store password", c -> c.keyStore("other")),
                Map.entry("key store type", c -> c.keystoreType("JKS")),
                Map.entry("key store instance", c -> c.keyStore(keyStore)),
                Map.entry("trust store path", c -> c.trustStore(SERVER_KEY_STORE, PASSWORD)),
                Map.entry("trust store type", c -> c.trustStoreType("JKS")),
                Map.entry("trust store instance", c -> c.trustStore(keyStore)),
                Map.entry("SSL context", c -> c.sslContext(sslContext)),
                Map.entry("relaxed validation", SSLConfig::relaxedHTTPSValidation),
                Map.entry("relaxed validation protocol", c -> c.relaxedHTTPSValidation("TLS")),
                Map.entry("all host names", SSLConfig::allowAllHostnames),
                Map.entry("host name verifier", c -> c.hostnameVerifier(CUSTOM_VERIFIER)));
        TlsSettings base = ClientSettingsFactory.tlsSettings(sslConfig());

        changes.forEach((name, change) -> {
            TlsSettings changed = ClientSettingsFactory.tlsSettings(change.apply(sslConfig()));
            assertThat(changed).as(name).isNotEqualTo(base).isEqualTo(ClientSettingsFactory.tlsSettings(change.apply(sslConfig())));
        });
    }

    @Test
    void ssl_config_values_are_recorded_as_given() {
        SSLConfig config = sslConfig().keyStore(CLIENT_KEY_STORE, PASSWORD).keystoreType("PKCS12").trustStore(SERVER_KEY_STORE, "secret")
                .relaxedHTTPSValidation("TLSv1.2");

        TlsSettings tls = ClientSettingsFactory.tlsSettings(config);

        assertThat(tls.keyStore()).isEqualTo(new KeyMaterial(CLIENT_KEY_STORE, PASSWORD, "PKCS12", null));
        assertThat(tls.trustStore()).isEqualTo(new KeyMaterial(SERVER_KEY_STORE, "secret", KeyStore.getDefaultType(), null));
        assertThat(tls.relaxedProtocol()).isEqualTo("TLSv1.2");
        assertThat(tls.sslContext()).isNull();
    }

    @Test
    void an_ssl_context_replaces_relaxed_validation() throws Exception {
        assertThat(ClientSettingsFactory.tlsSettings(sslConfig().relaxedHTTPSValidation().sslContext(SSLContext.getDefault())).relaxedProtocol()).isNull();
    }

    @Test
    void host_names_are_checked_as_the_ssl_config_says() {
        assertThat(ClientSettingsFactory.tlsSettings(sslConfig()).hostnames()).isEqualTo(HostnameVerification.STRICT);
        assertThat(ClientSettingsFactory.tlsSettings(sslConfig().allowAllHostnames()).hostnames()).isEqualTo(HostnameVerification.ALLOW_ALL);
        assertThat(ClientSettingsFactory.tlsSettings(sslConfig().allowAllHostnames().strictHostnames()).hostnames()).isEqualTo(HostnameVerification.STRICT);
        TlsSettings custom = ClientSettingsFactory.tlsSettings(sslConfig().hostnameVerifier(CUSTOM_VERIFIER));
        assertThat(custom.hostnames()).isEqualTo(HostnameVerification.CUSTOM);
        assertThat(custom.hostnameVerifier()).isSameAs(CUSTOM_VERIFIER);
    }

    @Test
    void relaxed_validation_reads_the_key_store_when_the_context_is_created() {
        // A key store set after relaxedHTTPSValidation() is used, and a wrong password fails when the request is sent
        SSLConfig config = sslConfig().relaxedHTTPSValidation().keyStore(CLIENT_KEY_STORE, "wrong");
        TlsSettings tls = ClientSettingsFactory.tlsSettings(config);

        assertThat(tls.keyStore().path()).isEqualTo(CLIENT_KEY_STORE);
        assertThat(catchThrowable(tls::createSslContext)).isInstanceOf(IOException.class);
    }

    @Test
    void creates_ssl_contexts_from_the_stores() throws Exception {
        SSLConfig stores = sslConfig().keyStore(CLIENT_KEY_STORE, PASSWORD).keystoreType("PKCS12").trustStore(SERVER_KEY_STORE, PASSWORD).trustStoreType("PKCS12");

        assertThat(ClientSettingsFactory.tlsSettings(stores).createSslContext().getProtocol()).isEqualTo("TLS");
        assertThat(ClientSettingsFactory.tlsSettings(stores.relaxedHTTPSValidation()).createSslContext().getProtocol()).isEqualTo("SSL");
    }

    @Test
    void a_missing_store_type_is_fine_when_the_store_isnt_read_from_a_path() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        CertAuthScheme scheme = new CertAuthScheme();
        scheme.setKeystoreType(null);
        scheme.setTrustStoreType(null);
        scheme.setSslContext(sslContext);

        assertThat(ClientSettingsFactory.tlsSettings(sslConfig().trustStoreType(null).trustStore(trustStore)).createSslContext()).isNotNull();
        assertThat(certificateTlsSettings(scheme).createSslContext()).isSameAs(sslContext);
        assertThat(ClientSettingsFactory.certificateTlsSettings(null, null, null, null, null, null, null, null, null, sslContext).createSslContext())
                .isSameAs(sslContext);
    }

    // Certificate authentication

    @Test
    void certificate_of_restassured_keeps_the_relaxed_validation_of_the_static_ssl_config() {
        RestAssured.config = RestAssured.config().sslConfig(sslConfig().relaxedHTTPSValidation("TLS"));
        try {
            // The key store isn't read yet, so a wrong password doesn't fail here
            Object scheme = RestAssured.certificate(CLIENT_KEY_STORE, "wrong");

            assertThat(scheme).isInstanceOf(RelaxedCertAuthScheme.class);
            TlsSettings tls = certificateTlsSettings(scheme);
            assertThat(tls.relaxedProtocol()).isEqualTo("TLS");
            assertThat(tls.sslContext()).isNull();
            assertThat(tls.keyStore().path()).isEqualTo(CLIENT_KEY_STORE);
            assertThat(catchThrowable(tls::createSslContext)).isInstanceOf(IOException.class);
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    void relaxed_certificate_scheme_can_only_be_applied_to_rest_assureds_own_authentication_context() {
        CertAuthScheme scheme = new CertAuthScheme();
        scheme.setPathToKeyStore(CLIENT_KEY_STORE);
        RelaxedCertAuthScheme relaxed = new RelaxedCertAuthScheme(scheme, "SSL");

        Throwable thrown = catchThrowable(() -> relaxed.authenticate(mock(AuthenticationContext.class)));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageStartingWith(
                "Certificate authentication with the relaxed HTTPS validation of RestAssured.config can't be applied to ");
    }

    @Test
    void an_ssl_context_set_on_the_relaxed_certificate_scheme_replaces_relaxed_validation() throws Exception {
        RelaxedCertAuthScheme relaxed = new RelaxedCertAuthScheme(new CertAuthScheme(), "SSL");
        relaxed.setSslContext(SSLContext.getDefault());

        assertThat(relaxed.getRelaxedProtocol()).isNull();
        assertThat(certificateTlsSettings(relaxed).relaxedProtocol()).isNull();
    }

    @Test
    void certificate_settings_replace_the_ssl_config() {
        TlsSettings certificate = certificateTlsSettings(RestAssured.certificate(SERVER_KEY_STORE, PASSWORD));

        ClientSettings settings = ClientSettingsFactory.clientSettings(sslConfig().relaxedHTTPSValidation(), certificate, null, HttpClientConfig.httpClientConfig());

        assertThat(settings.tls()).isSameAs(certificate);
    }

    @Test
    void every_certificate_auth_setting_that_the_settings_hold_changes_them() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Map<String, UnaryOperator<CertificateAuthSettings>> changes = Map.of(
                "key store type", s -> s.keyStoreType("JKS"),
                "trust store type", s -> s.trustStoreType("JKS"),
                "key store instance", s -> s.keyStore(keyStore),
                "trust store instance", s -> s.trustStore(keyStore),
                "all host names", CertificateAuthSettings::allowAllHostnames,
                "host name verifier", s -> s.hostnameVerifier(CUSTOM_VERIFIER),
                "SSL context", s -> s.sslContext(sslContext));
        TlsSettings base = certificateTlsSettings(RestAssured.certificate(SERVER_KEY_STORE, PASSWORD, certAuthSettings()));

        assertThat(certificateTlsSettings(RestAssured.certificate(SERVER_KEY_STORE, PASSWORD, certAuthSettings()))).isEqualTo(base);
        assertThat(certificateTlsSettings(RestAssured.certificate(SERVER_KEY_STORE, "other", certAuthSettings()))).isNotEqualTo(base);
        changes.forEach((name, change) -> {
            TlsSettings changed = certificateTlsSettings(RestAssured.certificate(SERVER_KEY_STORE, PASSWORD, change.apply(certAuthSettings())));
            assertThat(changed).as(name).isNotEqualTo(base)
                    .isEqualTo(certificateTlsSettings(RestAssured.certificate(SERVER_KEY_STORE, PASSWORD, change.apply(certAuthSettings()))));
        });
    }

    @Test
    void certificate_without_host_name_verifier_allows_all_host_names_for_stores_and_checks_them_strictly_for_a_context() throws Exception {
        TlsSettings stores = ClientSettingsFactory.certificateTlsSettings(SERVER_KEY_STORE, PASSWORD, "PKCS12", null, SERVER_KEY_STORE, PASSWORD, "PKCS12", null, null, null);
        TlsSettings context = ClientSettingsFactory.certificateTlsSettings(null, null, "PKCS12", null, null, null, "PKCS12", null, null, SSLContext.getDefault());

        assertThat(stores.hostnames()).isEqualTo(HostnameVerification.ALLOW_ALL);
        assertThat(context.hostnames()).isEqualTo(HostnameVerification.STRICT);
        assertThat(stores.createSslContext().getProtocol()).isEqualTo("TLS");
    }

    // Proxy, timeouts and request options

    @Test
    void translates_the_proxy() {
        ProxySpecification proxy = ProxySpecification.host("proxy.example").withPort(8888).withScheme("https").withAuth("user", "secret");

        assertThat(ClientSettingsFactory.proxySettings(proxy)).isEqualTo(new ProxySettings("https", "proxy.example", 8888, "user", "secret"));
        assertThat(ClientSettingsFactory.proxySettings(null)).isNull();
    }

    @Test
    void translates_the_http_client_config() {
        HttpClientConfig config = HttpClientConfig.httpClientConfig().connectTimeout(Duration.ofSeconds(1)).connectionRequestTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(3)).expectContinue(true);
        NtlmCredentials ntlm = new NtlmCredentials("host", 80, "user", "secret", "workstation", "domain");

        ClientSettings settings = ClientSettingsFactory.clientSettings(sslConfig(), null, ProxySpecification.host("proxy.example"), config);

        assertThat(settings).isEqualTo(new ClientSettings(null, new ProxySettings("http", "proxy.example", 8888, null, null), HttpVersion.HTTP_1_1,
                Duration.ofSeconds(1), Duration.ofSeconds(2), List.of()));
        assertThat(ClientSettingsFactory.requestOptions(config, ntlm)).isEqualTo(new RequestOptions(Duration.ofSeconds(3), true, ntlm));
        assertThat(ClientSettingsFactory.requestOptions(HttpClientConfig.httpClientConfig(), null)).isEqualTo(RequestOptions.DEFAULT);
    }

    // Key material, as created from the paths, passwords and types of the SSL config and certificate authentication

    @Test
    void key_material_without_a_path_loads_nothing_but_checks_the_type() throws Exception {
        assertThat(keyMaterial(null, PASSWORD).load()).isNull();
        assertThat(keyMaterial("", PASSWORD).load()).isNull();
        assertThat(catchThrowable(() -> ClientSettingsFactory.keyMaterial(null, PASSWORD, "not-a-key-store-type", null).load()))
                .isExactlyInstanceOf(KeyStoreException.class);
    }

    @Test
    void key_material_loads_from_the_class_path_with_or_without_a_leading_slash() throws Exception {
        KeyStore keyStore = keyMaterial(SERVER_KEY_STORE, PASSWORD).load();

        assertThat(keyStore.getType()).isEqualToIgnoringCase("PKCS12");
        assertThat(keyStore.size()).isEqualTo(1);
        assertThat(keyMaterial("/" + SERVER_KEY_STORE, PASSWORD).load().size()).isEqualTo(1);
    }

    @Test
    void key_material_loads_from_a_file_or_a_file_path_that_isnt_in_the_class_path() throws Exception {
        File file = new File(getClass().getClassLoader().getResource(SERVER_KEY_STORE).toURI());

        assertThat(keyMaterial(file, PASSWORD).load().size()).isEqualTo(1);
        assertThat(keyMaterial(file.getAbsolutePath(), PASSWORD).load().size()).isEqualTo(1);
    }

    @Test
    void key_material_takes_a_groovy_gstring_path_as_a_string() throws Exception {
        Object path = new GStringImpl(new Object[]{"self_signed_localhost"}, new String[]{"", ".p12"});

        KeyMaterial keyMaterial = keyMaterial(path, PASSWORD);

        assertThat(keyMaterial.path()).isEqualTo(SERVER_KEY_STORE);
        assertThat(keyMaterial.load().size()).isEqualTo(1);
    }

    @Test
    void key_material_rejects_a_path_that_is_neither_a_string_nor_a_file() {
        assertThat(catchThrowable(() -> keyMaterial(Paths.get(SERVER_KEY_STORE), PASSWORD))).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("The path to a key store or trust store must be a String or a java.io.File but was ");
    }

    @Test
    void key_material_that_cant_be_loaded_throws_the_exception_of_the_key_store() {
        assertThat(catchThrowable(() -> keyMaterial(new File("does-not-exist.p12"), PASSWORD).load())).isExactlyInstanceOf(FileNotFoundException.class);
        assertThat(catchThrowable(() -> keyMaterial(SERVER_KEY_STORE, "wrong password").load())).isExactlyInstanceOf(IOException.class);
    }

    private static KeyMaterial keyMaterial(Object path, String password) {
        return ClientSettingsFactory.keyMaterial(path, password, "PKCS12", null);
    }

    private static ClientSettings clientSettings(SSLConfig sslConfig) {
        return ClientSettingsFactory.clientSettings(sslConfig, null, null, HttpClientConfig.httpClientConfig());
    }

    private static TlsSettings certificateTlsSettings(Object scheme) {
        return ClientSettingsFactory.certificateTlsSettings((CertAuthScheme) scheme);
    }
}
