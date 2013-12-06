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

package com.jayway.restassured.internal.http;

import com.jayway.restassured.authentication.KeystoreProvider;
import com.jayway.restassured.internal.util.SafeExceptionRethrower;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.HttpRequestAdapter;
import oauth.signpost.exception.OAuthException;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;

/**
 * Encapsulates all configuration related to HTTP authentication methods.
 *
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 * @see HTTPBuilder#getAuth()
 */
public class AuthConfig {
    protected HTTPBuilder builder;

    public AuthConfig(HTTPBuilder builder) {
        this.builder = builder;
    }

    /**
     * Set authentication credentials to be used for the current
     * {@link HTTPBuilder#getUri() default host}.  This method name is a bit of
     * a misnomer, since these credentials will actually work for "digest"
     * authentication as well.
     *
     * @param user
     * @param pass
     */
    public void basic(String user, String pass) {
        URI uri = ((URIBuilder) builder.getUri()).toURI();
        if (uri == null) throw new IllegalStateException("a default URI must be set");
        this.basic(uri.getHost(), uri.getPort(), user, pass);
    }

    /**
     * Set authentication credentials to be used for the given host and port.
     *
     * @param host
     * @param port
     * @param user
     * @param pass
     */
    public void basic(String host, int port, String user, String pass) {
        builder.getClient().getCredentialsProvider().setCredentials(
                new AuthScope(host, port),
                new UsernamePasswordCredentials(user, pass)
        );
    }

    /**
     * Sets a certificate to be used for SSL authentication. See {@link Class#getResource(String)} for how to get a URL from a resource
     * on the classpath. The hostname of the server will be checked against the certificate subject.
     *
     * @param certURL            URL to a JKS keystore where the certificate is stored.
     * @param password           password to decrypt the keystore
     * @param certType           The certificate type
     * @param port               The SSL port
     * @param trustStoreProvider The provider
     */
    public void certificate(String certURL, String password, String certType, int port, KeystoreProvider trustStoreProvider) {
        certificate(certURL, password, certType, port, trustStoreProvider, true);
    }

    /**
     * Sets a certificate to be used for SSL authentication. See {@link Class#getResource(String)} for how to get a URL from a resource
     * on the classpath.
     *
     * @param certURL             URL to a JKS keystore where the certificate is stored.
     * @param password            password to decrypt the keystore
     * @param certType            The certificate type
     * @param port                The SSL port
     * @param trustStoreProvider  The provider
     * @param checkServerHostname Whether to check if the certificate subject matches the server hostname
     */
    public void certificate(String certURL, String password, String certType, int port, KeystoreProvider trustStoreProvider, boolean checkServerHostname) {
        try {
            KeyStore keyStore = KeyStore.getInstance(certType);
            InputStream jksStream = new URL(certURL).openStream();
            try {
                keyStore.load(jksStream, password.toCharArray());
            } finally {
                jksStream.close();
            }

            final SSLSocketFactory ssl;
            if (trustStoreProvider == null || !trustStoreProvider.canBuild()) {
                ssl = new SSLSocketFactory(keyStore, password);
            } else {
                ssl = new SSLSocketFactory(keyStore, password, trustStoreProvider.build());

            }

            ssl.setHostnameVerifier(checkServerHostname ? SSLSocketFactory.STRICT_HOSTNAME_VERIFIER : SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            SchemeRegistry registry = builder.getClient().getConnectionManager().getSchemeRegistry();
            registry.register(new Scheme("https", ssl, port));
        } catch (Exception e) {
            SafeExceptionRethrower.safeRethrow(e);
        }
    }

    /**
     * </p>OAuth sign all requests.  Note that this currently does <strong>not</strong>
     * wait for a <code>WWW-Authenticate</code> challenge before sending the
     * the OAuth header.  All requests to all domains will be signed for this
     * instance.</p>
     * <p/>
     * <p>This assumes you've already generated an <code>accessToken</code> and
     * <code>secretToken</code> for the site you're targeting.  For More information
     * on how to achieve this, see the
     * <a href='http://code.google.com/p/oauth-signpost/wiki/GettingStarted#Using_Signpost'>Signpost documentation</a>.</p>
     *
     * @param consumerKey    <code>null</code> if you want to <strong>unset</strong>
     *                       OAuth handling and stop signing requests.
     * @param consumerSecret
     * @param accessToken
     * @param secretToken
     * @since 0.5.1
     */
    public void oauth(String consumerKey, String consumerSecret,
                      String accessToken, String secretToken) {
        this.builder.client.removeRequestInterceptorByClass(OAuthSigner.class);
        if (consumerKey != null)
            this.builder.client.addRequestInterceptor(new OAuthSigner(
                    consumerKey, consumerSecret, accessToken, secretToken));
    }

    /**
     * This class is used to sign all requests via an {@link HttpRequestInterceptor}
     * until the context-aware AuthScheme is released in HttpClient 4.1.
     *
     * @since 0.5.1
     */
    static class OAuthSigner implements HttpRequestInterceptor {
        protected OAuthConsumer oauth;

        public OAuthSigner(String consumerKey, String consumerSecret,
                           String accessToken, String secretToken) {
            this.oauth = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
            oauth.setTokenWithSecret(accessToken, secretToken);
        }

        public void process(HttpRequest request, HttpContext ctx) {
            /* The full request URI must be reconstructed between the context and the request URI.
             * Best we can do until AuthScheme supports HttpContext.  See:
			 * https://issues.apache.org/jira/browse/HTTPCLIENT-901 */
            try {
                HttpUriRequest uriRequest = (HttpUriRequest) request;
                HttpHost host = (HttpHost) ctx.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

                final URI requestURI = new URI(host.toURI()).resolve(uriRequest.getURI());
                HttpRequestAdapter oAuthRequest = new HttpRequestAdapter(uriRequest) {
                    /* @Override */
                    public String getRequestUrl() {
                        return requestURI.toString();
                    }
                };
                this.oauth.sign(oAuthRequest);
            } catch (ClassCastException ex) {
                SafeExceptionRethrower.safeRethrow(new HttpException("Request must be an instance of HttpUriRequest", ex));
            } catch (URISyntaxException ex) {
                SafeExceptionRethrower.safeRethrow(new HttpException("Error rebuilding request URI", ex));
            } catch (OAuthException e) {
                SafeExceptionRethrower.safeRethrow(new HttpException("OAuth signing error", e));
            }
        }
    }
}