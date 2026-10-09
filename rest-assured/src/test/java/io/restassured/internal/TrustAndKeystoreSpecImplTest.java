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

package io.restassured.internal;

import io.restassured.internal.http.HTTPBuilder;
import io.restassured.internal.http.LazySSLSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.codehaus.groovy.runtime.GStringImpl;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;

import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
import static org.apache.http.conn.ssl.SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrustAndKeystoreSpecImplTest {

    // Self-signed certificate for localhost/127.0.0.1 in the test classpath
    private static final String KEYSTORE = "self_signed_localhost.p12";
    private static final String PASSWORD = "changeit";

    private final DefaultHttpClient client = new DefaultHttpClient();
    private final HTTPBuilder builder = mock(HTTPBuilder.class);

    {
        when(builder.getClient()).thenReturn(client);
    }

    @Test
    void registers_https_scheme_with_eager_factory_and_given_port_when_port_is_minus_one() {
        SSLSocketFactory factory = SSLSocketFactory.getSocketFactory();
        TrustAndKeystoreSpecImpl spec = new TrustAndKeystoreSpecImpl();
        spec.setPort(-1);
        spec.setFactory(factory);

        spec.apply(builder, 8443);

        Scheme https = httpsScheme();
        assertThat(https.getDefaultPort()).isEqualTo(8443);
        assertThat(https.getSchemeSocketFactory()).isSameAs(factory);
        // A factory that is given is used as is
        assertThat(factory.getHostnameVerifier()).isNotSameAs(ALLOW_ALL_HOSTNAME_VERIFIER);
    }

    @Test
    void registers_https_scheme_with_configured_port_when_port_is_not_minus_one() {
        TrustAndKeystoreSpecImpl spec = new TrustAndKeystoreSpecImpl();
        spec.setPort(9443);
        spec.setFactory(SSLSocketFactory.getSocketFactory());

        spec.apply(builder, 8443, false);

        assertThat(httpsScheme().getDefaultPort()).isEqualTo(9443);
    }

    @Test
    void port_is_zero_by_default_which_is_used_instead_of_the_given_port_and_is_invalid() {
        TrustAndKeystoreSpecImpl spec = new TrustAndKeystoreSpecImpl();
        spec.setFactory(SSLSocketFactory.getSocketFactory());

        Throwable thrown = catchThrowable(() -> spec.apply(builder, 8443));

        assertThat(spec.getPort()).isZero();
        assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class).hasMessage("Port is invalid");
    }

    @Test
    void lazy_apply_creates_the_factory_when_first_used() throws Exception {
        TrustAndKeystoreSpecImpl spec = spec(-1);
        spec.setTrustStorePath(KEYSTORE);
        spec.setTrustStorePassword(PASSWORD);

        spec.apply(builder, 443, true);

        assertThat(httpsScheme().getSchemeSocketFactory()).isInstanceOf(LazySSLSocketFactory.class);
        assertThat(spec.getFactory()).isNull();
        httpsScheme().getSchemeSocketFactory().createSocket(new BasicHttpParams()).close();
        assertThat(spec.getFactory()).isNotNull();
    }

    @Test
    void lazy_apply_wraps_failures_to_create_the_factory_in_an_ssl_exception() {
        TrustAndKeystoreSpecImpl spec = spec(-1);
        spec.setTrustStorePath("does-not-exist.p12");
        spec.setTrustStorePassword(PASSWORD);

        spec.apply(builder, 443, true);

        Throwable thrown = catchThrowable(() -> httpsScheme().getSchemeSocketFactory().createSocket(new BasicHttpParams()));
        // The checked exception thrown when creating the factory is wrapped in an UndeclaredThrowableException (without
        // message) since it's thrown through a Supplier
        assertThat(thrown).isExactlyInstanceOf(SSLException.class).
                hasMessage("Failed to create the SSL socket factory from the configured SSL settings (SSLConfig or certificate authentication): null").
                hasCauseExactlyInstanceOf(UndeclaredThrowableException.class).
                hasRootCauseExactlyInstanceOf(FileNotFoundException.class);
    }

    @Test
    void creates_factory_from_trust_store_in_classpath_with_allow_all_hostname_verifier_by_default() {
        TrustAndKeystoreSpecImpl spec = spec(-1);
        spec.setTrustStorePath(KEYSTORE);
        spec.setTrustStorePassword(PASSWORD);

        spec.apply(builder, 443);

        SSLSocketFactory factory = (SSLSocketFactory) httpsScheme().getSchemeSocketFactory();
        assertThat(factory.getHostnameVerifier()).isSameAs(ALLOW_ALL_HOSTNAME_VERIFIER);
        assertThat(spec.getFactory()).isSameAs(factory);
    }

    @Test
    void creates_factory_with_the_given_hostname_verifier() {
        TrustAndKeystoreSpecImpl spec = spec(-1);
        spec.setTrustStorePath(KEYSTORE);
        spec.setTrustStorePassword(PASSWORD);
        spec.setX509HostnameVerifier(STRICT_HOSTNAME_VERIFIER);

        spec.apply(builder, 443);

        assertThat(spec.getFactory().getHostnameVerifier()).isSameAs(STRICT_HOSTNAME_VERIFIER);
    }

    @Test
    void reuses_the_created_factory_when_applied_again() {
        TrustAndKeystoreSpecImpl spec = spec(-1);
        spec.setTrustStorePath(KEYSTORE);
        spec.setTrustStorePassword(PASSWORD);

        spec.apply(builder, 443);
        SSLSocketFactory first = spec.getFactory();
        spec.apply(builder, 443);

        assertThat(httpsScheme().getSchemeSocketFactory()).isSameAs(first);
    }

    @Test
    void creates_default_factory_with_allow_all_hostname_verifier_without_trust_store() {
        TrustAndKeystoreSpecImpl spec = spec(-1);

        spec.apply(builder, 443);

        assertThat(spec.getFactory()).isNotNull();
        assertThat(spec.getFactory().getHostnameVerifier()).isSameAs(ALLOW_ALL_HOSTNAME_VERIFIER);
    }

    @Test
    void loads_but_ignores_the_key_store_without_trust_store() {
        TrustAndKeystoreSpecImpl spec = spec(-1);
        spec.setKeyStorePath(KEYSTORE);
        spec.setKeyStorePassword(PASSWORD);

        spec.apply(builder, 443);

        assertThat(spec.getFactory()).isNotNull();
        assertThat(spec.getFactory().getHostnameVerifier()).isSameAs(ALLOW_ALL_HOSTNAME_VERIFIER);
    }

    @Test
    void loads_the_key_store_even_without_trust_store() {
        TrustAndKeystoreSpecImpl spec = spec(-1);
        spec.setKeyStorePath(KEYSTORE);
        spec.setKeyStorePassword("wrong password");

        Throwable thrown = catchThrowable(() -> spec.apply(builder, 443));

        assertThat(thrown).isExactlyInstanceOf(IOException.class);
    }

    @Test
    void uses_the_given_trust_store_and_key_store_instead_of_their_paths() throws Exception {
        TrustAndKeystoreSpecImpl spec = spec(-1);
        spec.setTrustStore(load(KEYSTORE));
        spec.setTrustStoreType("not-a-key-store-type");
        spec.setTrustStorePath("does-not-exist.p12");
        spec.setKeyStore(load(KEYSTORE));
        spec.setKeyStoreType("not-a-key-store-type");
        spec.setKeyStorePath("does-not-exist.p12");
        spec.setKeyStorePassword(PASSWORD);

        spec.apply(builder, 443);

        assertThat(spec.getFactory()).isNotNull();
    }

    @Test
    void loads_key_store_from_path_when_no_key_store_is_given() throws Exception {
        TrustAndKeystoreSpecImpl spec = spec(-1);
        spec.setTrustStore(load(KEYSTORE));
        spec.setKeyStorePath(KEYSTORE);
        spec.setKeyStorePassword("wrong password");

        Throwable thrown = catchThrowable(() -> spec.apply(builder, 443));

        assertThat(thrown).isExactlyInstanceOf(IOException.class);
    }

    @Test
    void failure_to_load_trust_store_is_thrown_unchanged() {
        TrustAndKeystoreSpecImpl spec = spec(-1);
        spec.setTrustStorePath("does-not-exist.p12");
        spec.setTrustStorePassword(PASSWORD);

        Throwable thrown = catchThrowable(() -> spec.apply(builder, 443));

        assertThat(thrown).isExactlyInstanceOf(FileNotFoundException.class).hasMessageContaining("does-not-exist.p12");
    }

    @Test
    void create_store_returns_null_for_null_or_empty_path() throws Exception {
        TrustAndKeystoreSpecImpl spec = new TrustAndKeystoreSpecImpl();

        assertThat(spec.createStore("PKCS12", null, PASSWORD)).isNull();
        assertThat(spec.createStore("PKCS12", "", PASSWORD)).isNull();
    }

    @Test
    void create_store_validates_the_type_before_looking_at_the_path() {
        Throwable thrown = catchThrowable(() -> new TrustAndKeystoreSpecImpl().createStore("not-a-key-store-type", null, PASSWORD));

        assertThat(thrown).isExactlyInstanceOf(KeyStoreException.class);
    }

    @Test
    void create_store_accepts_gstring_type_and_password() throws Exception {
        Object type = new GStringImpl(new Object[]{"PKCS"}, new String[]{"", "12"});
        Object password = new GStringImpl(new Object[]{PASSWORD}, new String[]{"", ""});

        KeyStore keyStore = new TrustAndKeystoreSpecImpl().createStore(type, KEYSTORE, password);

        assertThat(keyStore.size()).isEqualTo(1);
    }

    @Test
    void create_store_loads_from_classpath() throws Exception {
        KeyStore keyStore = new TrustAndKeystoreSpecImpl().createStore("PKCS12", KEYSTORE, PASSWORD);

        assertThat(keyStore.getType()).isEqualToIgnoringCase("PKCS12");
        assertThat(keyStore.size()).isEqualTo(1);
    }

    @Test
    void create_store_loads_from_classpath_with_leading_slash() throws Exception {
        KeyStore keyStore = new TrustAndKeystoreSpecImpl().createStore("PKCS12", "/" + KEYSTORE, PASSWORD);

        assertThat(keyStore.size()).isEqualTo(1);
    }

    @Test
    void create_store_loads_from_file() throws Exception {
        File file = new File(getClass().getClassLoader().getResource(KEYSTORE).toURI());

        KeyStore keyStore = new TrustAndKeystoreSpecImpl().createStore("PKCS12", file, PASSWORD);

        assertThat(keyStore.size()).isEqualTo(1);
    }

    @Test
    void create_store_loads_from_file_path_not_in_classpath() throws Exception {
        String path = new File(getClass().getClassLoader().getResource(KEYSTORE).toURI()).getAbsolutePath();

        KeyStore keyStore = new TrustAndKeystoreSpecImpl().createStore("PKCS12", path, PASSWORD);

        assertThat(keyStore.size()).isEqualTo(1);
    }

    @Test
    void create_store_loads_from_classpath_path_given_as_gstring() throws Exception {
        Object path = new GStringImpl(new Object[]{"self_signed_localhost"}, new String[]{"", ".p12"});

        KeyStore keyStore = new TrustAndKeystoreSpecImpl().createStore("PKCS12", path, PASSWORD);

        assertThat(keyStore.size()).isEqualTo(1);
    }

    @Test
    void create_store_throws_illegal_argument_exception_for_path_that_is_neither_string_nor_file() {
        Throwable thrown = catchThrowable(() -> new TrustAndKeystoreSpecImpl().createStore("PKCS12", Paths.get(KEYSTORE), PASSWORD));

        assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class).
                hasMessageStartingWith("The path to a key store or trust store must be a String or a java.io.File but was ");
    }

    @Test
    void create_store_throws_file_not_found_exception_for_missing_file() {
        Throwable thrown = catchThrowable(() -> new TrustAndKeystoreSpecImpl().createStore("PKCS12", new File("does-not-exist.p12"), PASSWORD));

        assertThat(thrown).isExactlyInstanceOf(FileNotFoundException.class);
    }

    @Test
    void create_store_throws_io_exception_for_wrong_password() {
        Throwable thrown = catchThrowable(() -> new TrustAndKeystoreSpecImpl().createStore("PKCS12", KEYSTORE, "wrong password"));

        assertThat(thrown).isExactlyInstanceOf(IOException.class);
    }

    private static TrustAndKeystoreSpecImpl spec(int port) {
        TrustAndKeystoreSpecImpl spec = new TrustAndKeystoreSpecImpl();
        spec.setPort(port);
        spec.setKeyStoreType("PKCS12");
        spec.setTrustStoreType("PKCS12");
        return spec;
    }

    private static KeyStore load(String path) throws Exception {
        return new TrustAndKeystoreSpecImpl().createStore("PKCS12", path, PASSWORD);
    }

    private Scheme httpsScheme() {
        return client.getConnectionManager().getSchemeRegistry().getScheme("https");
    }
}
