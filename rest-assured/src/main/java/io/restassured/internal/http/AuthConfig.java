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

package io.restassured.internal.http;

import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.oauth.OAuthService;
import io.restassured.authentication.OAuthSignature;
import io.restassured.internal.TrustAndKeystoreSpecImpl;
import org.apache.commons.lang3.EnumUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.EntityEnclosingRequestWrapper;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.util.List;
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
    private final io.restassured.config.OAuthConfig raOAuthConfig;

    public AuthConfig(HTTPBuilder builder, io.restassured.config.OAuthConfig restAssuredOAuthConfig) {
        this.builder = builder;
        this.raOAuthConfig = restAssuredOAuthConfig;
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
     * Set NTLM authentication credentials to be used for the current
     * {@link HTTPBuilder#getUri() default host}.
     *
     * @param user
     * @param pass
     */
    public void ntlm(String user, String pass, String workstation, String domain) {
        URI uri = ((URIBuilder) builder.getUri()).toURI();
        if (uri == null) throw new IllegalStateException("a default URI must be set");
        this.ntlm(uri.getHost(), uri.getPort(), user, pass,workstation,domain);
    }
    /**
     * Set NTLM authentication credentials to be used for the given host and port.
     *
     * @param host
     * @param port
     * @param user
     * @param pass
     * @param workstation
     * @param domain
     */
    public void ntlm(String host, int port, String user, String pass, String workstation, String domain) {
        builder.getClient().getCredentialsProvider().setCredentials(
                new AuthScope(host, port),
                new NTCredentials(user, pass, workstation, domain)
        );
    }

    /**
     * Sets a certificate to be used for SSL authentication. See {@link Class#getResource(String)} for how to get a URL from a resource
     * on the classpath.
     *
     * @param keyStorePath               URL to a JKS keystore where the certificate is stored.
     * @param keyStorePassword           password to decrypt the keystore
     * @param keyStoreType               The certificate type
     * @param keyStore                   The key store
     * @param trustStorePath             URL to a trust store
     * @param trustStorePassword         password to decrypt the trust store
     * @param trustStoreType             The certificate type
     * @param trustStore                 The trust store
     * @param port                       The SSL port
     * @param hostnameVerifier           The X509HostnameVerifier to use
     * @param sslConnectionSocketFactory The SSLConnectionSocketFactory to use
     */
    public void certificate(Object keyStorePath, String keyStorePassword, String keyStoreType, KeyStore keyStore,
                            Object trustStorePath, String trustStorePassword, String trustStoreType, KeyStore trustStore,
                            int port, X509HostnameVerifier hostnameVerifier, SSLSocketFactory sslConnectionSocketFactory) {
        TrustAndKeystoreSpecImpl spec = new TrustAndKeystoreSpecImpl();
        URI uri = ((URIBuilder) builder.getUri()).toURI();
        if (uri == null) throw new IllegalStateException("a default URI must be set");
        spec.setKeyStoreType(keyStoreType);
        spec.setKeyStorePassword(keyStorePassword);
        spec.setKeyStorePath(keyStorePath);
        spec.setKeyStore(keyStore);
        spec.setTrustStoreType(trustStoreType);
        spec.setTrustStorePassword(trustStorePassword);
        spec.setTrustStorePath(trustStorePath);
        spec.setTrustStore(trustStore);
        spec.setPort(port);
        spec.setX509HostnameVerifier(hostnameVerifier);
        spec.setFactory(sslConnectionSocketFactory);

        int portSpecifiedInUri = uri.getPort();
        spec.apply(builder, portSpecifiedInUri == UNDEFINED_PORT ? DEFAULT_HTTPS_PORT : portSpecifiedInUri);
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
     * <a href='https://github.com/scribejava/scribejava/wiki/Getting-Started'>Scribe documentation</a>.</p>
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
                    consumerKey, consumerSecret, accessToken, secretToken, OAuthSignature.HEADER,
                    raOAuthConfig.shouldAddEmptyAccessOAuthTokenToBaseString()));
        }
    }


    public void oauth(String consumerKey, String consumerSecret,
                      String accessToken, String secretToken, OAuthSignature signature) {
        this.builder.client.removeRequestInterceptorByClass(OAuthSigner.class);
        if (consumerKey != null) {
            this.builder.client.addRequestInterceptor(new OAuthSigner(
                    consumerKey, consumerSecret, accessToken, secretToken,
                    signature, raOAuthConfig.shouldAddEmptyAccessOAuthTokenToBaseString()));
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
     * <a href='https://github.com/scribejava/scribejava/wiki/Getting-Started'>Scribe documentation</a>.</p>
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
            this.builder.client.addRequestInterceptor(new OAuthSigner(accessToken, signature));
        }
    }

    static class OAuthSigner implements HttpRequestInterceptor {
        protected OAuthConfig oauthConfig;
        protected Token token;
        protected OAuth10aService service;
        protected SignatureType type = SignatureType.Header;
        protected OAuthSignature signature;
        protected boolean isOAuth1 = true;
        protected boolean addEmptyTokenToBaseString;

        public OAuthSigner(String consumerKey, String consumerSecret,
                           String accessToken, String secretToken, OAuthSignature signature, Boolean addEmptyTokenToBaseString) {

            this.oauthConfig = new OAuthConfig(consumerKey, consumerSecret,
                    null, getOAuthSignatureType(signature), null, null, null, null, null, null, null);
            this.token = new OAuth1AccessToken(accessToken, secretToken);
            this.signature = signature;
            this.addEmptyTokenToBaseString = addEmptyTokenToBaseString;
        }

        public OAuthSigner(String accessToken, OAuthSignature signature) {
            this.token = new OAuth2AccessToken(accessToken, "");
            this.signature = signature;
            isOAuth1 = false;
        }

        public void process(HttpRequest request, HttpContext ctx) throws HttpException, IOException {
            try {
                Verb verb = EnumUtils.getEnum(Verb.class, request.getRequestLine().getMethod().toUpperCase());
                if (verb == null)
                    return;

                HttpHost host = (HttpHost) ctx.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                final URI requestURI = new URI(host.toURI()).resolve(request.getRequestLine().getUri());

                OAuthRequest oauthRequest = new OAuthRequest(verb, requestURI.toString(), null);
                this.service = (OAuth10aService) getOauthService(isOAuth1, addEmptyTokenToBaseString);

                if (request instanceof EntityEnclosingRequestWrapper) {
                    HttpEntity entity = ((EntityEnclosingRequestWrapper) request).getEntity();
                    if (entity != null) {
                        List<NameValuePair> params = URLEncodedUtils.parse(entity);
                        for (NameValuePair param : params) {
                            String value = param.getValue() == null ? "" : param.getValue();
                            oauthRequest.addBodyParameter(param.getName(), value);
                        }
                    }
                }

                service.signRequest((OAuth1AccessToken) token, oauthRequest);

                if (signature == OAuthSignature.HEADER) {
                    //If signature is to be added as header
                    for (Map.Entry<String, String> entry : oauthRequest.getHeaders().entrySet()) {
                        request.setHeader(entry.getKey(), entry.getValue());
                    }
                } else {
                    //If signature is to be added as query param
                    URI uri = new URI(oauthRequest.getCompleteUrl());
                    ((RequestWrapper) request).setURI(uri);
                }

            } catch (URISyntaxException ex) {
                throw new HttpException("Error rebuilding request URI", ex);
            }
        }

        private OAuthService getOauthService(boolean oauth1, final boolean useEmptyOAuthToken) {
            OAuthService service;
            if (oauth1) {
                DefaultApi10a api = new DefaultApi10a() {
                    @Override
                    public String getRequestTokenEndpoint() {
                        return null;
                    }

                    @Override
                    public String getAuthorizationUrl(OAuth1RequestToken arg0) {
                        return null;
                    }

                    @Override
                    public String getAccessTokenEndpoint() {
                        return null;
                    }

                    @Override
                    public boolean isEmptyOAuthTokenParamIsRequired() {
                        return useEmptyOAuthToken;
                    }
                };
                service = new OAuth10aService(api, oauthConfig);
            } else {
                DefaultApi20 api = new DefaultApi20() {
                    @Override
                    public String getAuthorizationUrl(OAuthConfig arg0) {
                        return null;
                    }

                    @Override
                    public String getAccessTokenEndpoint() {
                        return null;
                    }
                };
                service = new OAuth20Service(api, oauthConfig);
            }
            return service;
        }

        private static SignatureType getOAuthSignatureType(OAuthSignature signature) {
            SignatureType signatureType;
            if (signature == OAuthSignature.HEADER)
                signatureType = SignatureType.Header;
            else
                signatureType = SignatureType.QueryString;
            return signatureType;
        }


    }
}