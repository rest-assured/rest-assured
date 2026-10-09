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
package io.restassured.internal;

import io.restassured.RestAssured;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.BasicAuthScheme;
import io.restassured.authentication.CertificateAuthSettings;
import io.restassured.authentication.ExplicitNoAuthScheme;
import io.restassured.authentication.FormAuthConfig;
import io.restassured.authentication.FormAuthScheme;
import io.restassured.authentication.NTLMAuthScheme;
import io.restassured.authentication.OAuth2Scheme;
import io.restassured.authentication.OAuthScheme;
import io.restassured.authentication.OAuthSignature;
import io.restassured.authentication.PreemptiveOAuth2HeaderScheme;
import io.restassured.specification.AuthenticationSpecification;
import io.restassured.specification.PreemptiveAuthSpec;
import io.restassured.specification.RequestSpecification;
import io.restassured.spi.AuthFilter;

import static io.restassured.authentication.CertificateAuthSettings.certAuthSettings;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Specify an authentication scheme to use when sending a request.
 */
public class AuthenticationSpecificationImpl implements AuthenticationSpecification {
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private final RequestSpecification requestSpecification;

    public AuthenticationSpecificationImpl(RequestSpecification requestSpecification) {
        this.requestSpecification = requestSpecification;
    }

    /**
     * Use http basic authentication.
     *
     * @param userName The user name.
     * @param password The password.
     * @return The request builder
     */
    public RequestSpecification basic(String userName, String password) {
        notNull(userName, "userName");
        notNull(password, "password");

        BasicAuthScheme scheme = new BasicAuthScheme();
        scheme.setUserName(userName);
        scheme.setPassword(password);
        return use(scheme);
    }

    /**
     * Use NTLM authentication.
     *
     * @param userName The user name.
     * @param password The password.
     * @return The request builder
     */
    public RequestSpecification ntlm(String userName, String password, String workstation, String domain) {
        notNull(userName, "userName");
        notNull(password, "password");
        notNull(workstation, "workstation");
        notNull(domain, "domain");

        NTLMAuthScheme scheme = new NTLMAuthScheme();
        scheme.setUserName(userName);
        scheme.setPassword(password);
        scheme.setWorkstation(workstation);
        scheme.setDomain(domain);
        return use(scheme);
    }

    /**
     * Use http digest authentication.
     *
     * @param userName The user name.
     * @param password The password.
     * @return The request builder
     */
    public RequestSpecification digest(String userName, String password) {
        notNull(userName, "userName");
        notNull(password, "password");

        BasicAuthScheme scheme = new BasicAuthScheme();
        scheme.setUserName(userName);
        scheme.setPassword(password);
        return use(scheme);
    }

    /**
     * {@inheritDoc}
     */
    public RequestSpecification certificate(String certURL, String password) {
        return certificate(certURL, password, certAuthSettings());
    }

    /**
     * {@inheritDoc}
     */
    public RequestSpecification certificate(String certURL, String password, CertificateAuthSettings settings) {
        notNull(certURL, "certURL");
        notNull(password, "password");
        notNull(settings, CertificateAuthSettings.class);

        // RestAssured.certificate(..) reads the file as key store and trust store
        return use(RestAssured.certificate(certURL, password, settings));
    }

    /**
     * {@inheritDoc}
     */
    public RequestSpecification oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken) {
        notNull(consumerKey, "consumerKey");
        notNull(consumerSecret, "consumerSecret");
        notNull(accessToken, "accessToken");
        notNull(secretToken, "secretToken");

        return use(oauthScheme(consumerKey, consumerSecret, accessToken, secretToken));
    }

    /**
     * Excerpt from the HttpBuilder docs:<br>
     * OAuth sign the request. Note that this currently does not wait for a WWW-Authenticate challenge before sending the the OAuth header.
     * All requests to all domains will be signed for this instance.
     * This assumes you've already generated an accessToken and secretToken for the site you're targeting.
     * For More information on how to achieve this, see the <a href="https://github.com/mttkay/signpost/blob/master/docs/GettingStarted.md#using-signpost">Signpost documentation</a>.
     *
     * @param consumerKey
     * @param consumerSecret
     * @param accessToken
     * @param secretToken
     * @param signature
     * @return The request io.restassured.specification
     */
    public RequestSpecification oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken, OAuthSignature signature) {
        notNull(consumerKey, "consumerKey");
        notNull(consumerSecret, "consumerSecret");
        notNull(accessToken, "accessToken");
        notNull(secretToken, "secretToken");
        notNull(signature, "signature");

        OAuthScheme scheme = oauthScheme(consumerKey, consumerSecret, accessToken, secretToken);
        scheme.setSignature(signature);
        return use(scheme);
    }

    /**
     * {@inheritDoc}
     */
    public RequestSpecification oauth2(String accessToken) {
        return oauth2(accessToken, OAuthSignature.HEADER);
    }

    /**
     * {@inheritDoc}
     */
    public RequestSpecification oauth2(String accessToken, OAuthSignature signature) {
        notNull(accessToken, "accessToken");

        if (signature == OAuthSignature.HEADER) {
            PreemptiveOAuth2HeaderScheme scheme = new PreemptiveOAuth2HeaderScheme();
            scheme.setAccessToken(accessToken);
            return use(scheme);
        } else {
            OAuth2Scheme scheme = new OAuth2Scheme();
            scheme.setAccessToken(accessToken);
            scheme.setSignature(signature);
            return use(scheme);
        }
    }

    public RequestSpecification none() {
        RequestSpecificationImpl spec = requestSpecificationImpl();
        spec.setAuthenticationScheme(new ExplicitNoAuthScheme());
        spec.getFilters().removeIf(filter -> filter instanceof AuthFilter);
        spec.removeHeader(AUTHORIZATION_HEADER_NAME);
        return requestSpecification;
    }

    public PreemptiveAuthSpec preemptive() {
        return new PreemptiveAuthSpecImpl(requestSpecification);
    }

    public RequestSpecification form(String userName, String password) {
        return form(userName, password, null);
    }

    public RequestSpecification form(String userName, String password, FormAuthConfig config) {
        FormAuthScheme scheme = new FormAuthScheme();
        scheme.setUserName(userName);
        scheme.setPassword(password);
        scheme.setConfig(config);
        return use(scheme);
    }

    private static OAuthScheme oauthScheme(String consumerKey, String consumerSecret, String accessToken, String secretToken) {
        OAuthScheme scheme = new OAuthScheme();
        scheme.setConsumerKey(consumerKey);
        scheme.setConsumerSecret(consumerSecret);
        scheme.setAccessToken(accessToken);
        scheme.setSecretToken(secretToken);
        return scheme;
    }

    private RequestSpecification use(AuthenticationScheme scheme) {
        requestSpecificationImpl().setAuthenticationScheme(scheme);
        return requestSpecification;
    }

    private RequestSpecificationImpl requestSpecificationImpl() {
        return (RequestSpecificationImpl) requestSpecification;
    }
}
