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

import com.jayway.restassured.authentication.OAuthSignature;
import com.jayway.restassured.internal.KeystoreSpecImpl;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.builder.api.DefaultApi20;
import org.scribe.model.*;
import org.scribe.oauth.OAuth10aServiceImpl;
import org.scribe.oauth.OAuth20ServiceImpl;
import org.scribe.oauth.OAuthService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.Map;

/**
 * Encapsulates all configuration related to HTTP authentication methods.
 *
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 * @author johanhaleby
 */
public class AuthConfig {
    private static final int UNDEFINED_PORT = -1;
    private static final int DEFAULT_HTTPS_PORT = 443;
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
     * on the classpath.
     *
     * @param certURL              URL to a JKS keystore where the certificate is stored.
     * @param password             password to decrypt the keystore
     * @param certType             The certificate type
     * @param port                 The SSL port
     * @param trustStore           The trust store
     * @param x509HostnameVerifier The X509HostnameVerifier to use
     */
    public void certificate(String certURL, String password, String certType, int port, KeyStore trustStore, X509HostnameVerifier x509HostnameVerifier) {
        KeystoreSpecImpl keystoreSpec = new KeystoreSpecImpl();
        URI uri = ((URIBuilder) builder.getUri()).toURI();
        if (uri == null) throw new IllegalStateException("a default URI must be set");
        keystoreSpec.setKeyStoreType(certType);
        keystoreSpec.setPassword(password);
        keystoreSpec.setPath(certURL);
        keystoreSpec.setTrustStore(trustStore);
        keystoreSpec.setPort(port);
        keystoreSpec.setX509HostnameVerifier(x509HostnameVerifier);
        int portSpecifiedInUri = uri.getPort();
        keystoreSpec.apply(builder, portSpecifiedInUri == UNDEFINED_PORT ? DEFAULT_HTTPS_PORT : portSpecifiedInUri);
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
     * <a href='https://github.com/fernandezpablo85/scribe-java/wiki/Getting-Started'>Scribe documentation</a>.</p>
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
        if (consumerKey != null) {
            this.builder.client.addRequestInterceptor(new OAuthSigner(
                    consumerKey, consumerSecret, accessToken, secretToken, OAuthSignature.HEADER));
        }
    }

    public void oauth(String consumerKey, String consumerSecret,
                      String accessToken, String secretToken, OAuthSignature signature) {
        this.builder.client.removeRequestInterceptorByClass(OAuthSigner.class);
        if (consumerKey != null) {
            this.builder.client.addRequestInterceptor(new OAuthSigner(
                    consumerKey, consumerSecret, accessToken, secretToken,
                    signature));
        }
    }


    /**
     * </p>OAuth2 sign all requests.  Note that this currently does <strong>not</strong>
     * wait for a <code>WWW-Authenticate</code> challenge before sending the
     * the OAuth header.  All requests to all domains will be signed for this
     * instance.</p>
     * <p/>
     * <p>This assumes you've already generated an <code>accessToken</code>
     * for the site you're targeting.  For More information
     * on how to achieve this, see the
     * <a href='https://github.com/fernandezpablo85/scribe-java/wiki/Getting-Started'>Scribe documentation</a>.</p>
     *
     * @param accessToken
     * @since 0.5.1
     */
    public void oauth2(String accessToken) {
        this.builder.client.removeRequestInterceptorByClass(OAuthSigner.class);
        if (accessToken != null) {
            this.builder.client.addRequestInterceptor(new OAuthSigner(accessToken, OAuthSignature.HEADER));
        }
    }

    public void oauth2(String accessToken, OAuthSignature signature) {
        this.builder.client.removeRequestInterceptorByClass(OAuthSigner.class);
        if (accessToken != null) {
            this.builder.client.addRequestInterceptor(new OAuthSigner(
                    accessToken, signature));
        }
    }

    static class OAuthSigner implements HttpRequestInterceptor {
        protected OAuthConfig oauthConfig;
        protected Token token;
        protected OAuthService service;
        protected SignatureType type = SignatureType.Header;
        protected OAuthSignature signature;
        protected boolean oauth1 = true;

        public OAuthSigner(String consumerKey, String consumerSecret,
                           String accessToken, String secretToken, OAuthSignature signature) {

            this.oauthConfig = new OAuthConfig(consumerKey, consumerSecret,
                    null, getOAuthSigntureType(signature), null, null);
            this.token = new Token(accessToken, secretToken);
            this.signature = signature;
        }

        public OAuthSigner(String accessToken, OAuthSignature signature) {
            this.token = new Token(accessToken, "");
            this.signature = signature;
            oauth1 = false;

        }

        public void process(HttpRequest request, HttpContext ctx) throws HttpException, IOException {

            try {
                HttpHost host = (HttpHost) ctx
                        .getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                final URI requestURI = new URI(host.toURI()).resolve(request
                        .getRequestLine().getUri());

                OAuthRequest oauthRequest = new OAuthRequest(Verb.GET,
                        requestURI.toString());
                this.service = getOauthService(oauth1);
                service.signRequest(token, oauthRequest);
                if (signature == OAuthSignature.HEADER) {
                    //If signature is to be added as header
                    for (Map.Entry<String, String> entry : oauthRequest.getHeaders().entrySet()) {
                        request.setHeader(entry.getKey(), entry.getValue());
                    }
                } else {
                    //If signature is to be added as query param
                    URI uri = new URI(oauthRequest.getCompleteUrl());
                    HttpParams params = new BasicHttpParams();
                    for (NameValuePair entry : URLEncodedUtils.parse(uri, "UTF-8")) {
                        params.setParameter(entry.getName(), entry.getValue());

                    }
                    request.setParams(params);
                }

            } catch (URISyntaxException ex) {
                throw new HttpException("Error rebuilding request URI", ex);
            }
        }

        private OAuthService getOauthService(boolean oauth1) {
            OAuthService service = null;
            if (oauth1) {
                DefaultApi10a api = new DefaultApi10a() {
                    @Override
                    public String getRequestTokenEndpoint() {
                        return null;
                    }

                    @Override
                    public String getAuthorizationUrl(Token arg0) {
                        return null;
                    }

                    @Override
                    public String getAccessTokenEndpoint() {
                        return null;
                    }
                };
                service = new OAuth10aServiceImpl(api, oauthConfig);
            } else {
                DefaultApi20 api = new DefaultApi20() {

                    @Override
                    public String getAuthorizationUrl(OAuthConfig arg0) {
                        // TODO Auto-generated method stub
                        return null;
                    }

                    @Override
                    public String getAccessTokenEndpoint() {
                        // TODO Auto-generated method stub
                        return null;
                    }
                };
                service = new OAuth20ServiceImpl(api, oauthConfig);
            }
            return service;
        }

        private static SignatureType getOAuthSigntureType(OAuthSignature signature) {
            SignatureType signatureType;
            if (signature == OAuthSignature.HEADER)
                signatureType = SignatureType.Header;
            else
                signatureType = SignatureType.QueryString;
            return signatureType;
        }


    }


}