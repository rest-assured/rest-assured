package io.restassured;

import io.restassured.authentication.*;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Method;
import io.restassured.mapper.ObjectMapper;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.*;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;

/**
 * Created by andrey.smirnov on 26.04.2017.
 *
 */
public class RestAssuredThreadLocal {

    private static ThreadLocal<RestAssuredThreadLocalImpl> restAssuredThreadLocal = new ThreadLocal<RestAssuredThreadLocalImpl>() {
        @Override
        protected RestAssuredThreadLocalImpl initialValue() {
            return new RestAssuredThreadLocalImpl();
        }
    };

    private static RestAssuredThreadLocalImpl impl() {
        return restAssuredThreadLocal.get();
    }

    public static void filters(List<Filter> filters) {
        impl().filters(filters);
    }

    public static void proxy(String host) {
        impl().proxy(host);
    }

    public static AuthenticationScheme oauth2(String accessToken) {
        return impl().oauth2(accessToken);
    }

    public static Response put() {
        return impl().put();
    }

    public static AuthenticationScheme form(String userName, String password, FormAuthConfig config) {
        return impl().form(userName, password, config);
    }

    public static Response delete(String path, Map<String, ?> pathParams) {
        return impl().delete(path, pathParams);
    }

    public static String getBaseURI() {
        return impl().getBaseURI();
    }

    public static void useRelaxedHTTPSValidation(String protocol) {
        impl().useRelaxedHTTPSValidation(protocol);
    }

    public static List<Argument> withNoArguments() {
        return impl().withNoArguments();
    }

    public static String getBasePath() {
        return impl().getBasePath();
    }

    public static void enableLoggingOfRequestAndResponseIfValidationFails() {
        impl().enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public static List<Argument> withArguments(Object firstArgument, Object... additionalArguments) {
        return impl().withArguments(firstArgument, additionalArguments);
    }

    public static AuthenticationScheme setAuthentication(AuthenticationScheme authenticationScheme) {
        return impl().setAuthentication(authenticationScheme);
    }

    public static AuthenticationScheme getAuthentication() {
        return impl().getAuthentication();
    }

    public static Response head() {
        return impl().head();
    }

    public static RequestSender given(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        return impl().given(requestSpecification, responseSpecification);
    }

    public static AuthenticationScheme certificate(String certURL, String password, CertificateAuthSettings certificateAuthSettings) {
        return impl().certificate(certURL, password, certificateAuthSettings);
    }

    public static AuthenticationScheme certificate(String certURL, String password) {
        return impl().certificate(certURL, password);
    }

    public static AuthenticationScheme certificate(String trustStorePath, String trustStorePassword,
                                                   String keyStorePath, String keyStorePassword,
                                                   CertificateAuthSettings certificateAuthSettings) {
        return impl().certificate(trustStorePath, trustStorePassword, keyStorePath, keyStorePassword, certificateAuthSettings);
    }

    public static Response put(String path, Object... pathParams) {
        return impl().put(path, pathParams);
    }

    public static Response head(String path, Object... pathParams) {
        return impl().head(path, pathParams);
    }

    public static RequestSender when() {
        return impl().when();
    }

    public static Response patch(URI uri) {
        return impl().patch(uri);
    }

    public static Response post(URI uri) {
        return impl().post(uri);
    }

    public static void setBaseURI(String baseURI) {
        impl().setBaseURI(baseURI);
    }

    public static Response put(URL url) {
        return impl().put(url);
    }

    public static void keyStore(File pathToJks, String password) {
        impl().keyStore(pathToJks, password);
    }

    public static Response patch() {
        return impl().patch();
    }

    public static Response head(URL url) {
        return impl().head(url);
    }

    public static void unregisterParser(String contentType) {
        impl().unregisterParser(contentType);
    }

    public static void useRelaxedHTTPSValidation() {
        impl().useRelaxedHTTPSValidation();
    }

    public static RequestSpecification given(RequestSpecification requestSpecification) {
        return impl().given(requestSpecification);
    }

    public static Response put(URI uri) {
        return impl().put(uri);
    }

    public static Response delete(URI uri) {
        return impl().delete(uri);
    }

    public static Integer getPort() {
        return impl().getPort();
    }

    public static Response get(URI uri) {
        return impl().get(uri);
    }

    public static Response delete(String path, Object... pathParams) {
        return impl().delete(path, pathParams);
    }

    public static RequestSpecification given() {
        return impl().given();
    }

    public static Response options(URI uri) {
        return impl().options(uri);
    }

    public static void proxy(String host, int port) {
        impl().proxy(host, port);
    }

    public static AuthenticationScheme form(String userName, String password) {
        return impl().form(userName, password);
    }

    public static Response get(URL url) {
        return impl().get(url);
    }

    public static ResponseSpecification expect() {
        return impl().expect();
    }

    public static void keyStore(String password) {
        impl().keyStore(password);
    }

    public static Response head(URI uri) {
        return impl().head(uri);
    }

    public static void proxy(ProxySpecification proxySpecification) {
        impl().proxy(proxySpecification);
    }

    public static void setPort(int port) {
        impl().setPort(port);
    }

    public static Response patch(URL url) {
        return impl().patch(url);
    }

    public static void trustStore(KeyStore trustStore) {
        impl().trustStore(trustStore);
    }

    public static void trustStore(File pathToJks, String password) {
        impl().trustStore(pathToJks, password);
    }

    public static void trustStore(String pathToJks, String password) {
        impl().trustStore(pathToJks, password);
    }

    public static void proxy(String host, int port, String scheme) {
        impl().proxy(host, port, scheme);
    }

    public static Response options(String path, Object... pathParams) {
        return impl().options(path, pathParams);
    }

    public static RestAssuredConfig config() {
        return impl().config();
    }

    public static void replaceFiltersWith(Filter filter, Filter... additionalFilters) {
        impl().replaceFiltersWith(filter, additionalFilters);
    }

    public static Response get(String path, Map<String, ?> pathParams) {
        return impl().get(path, pathParams);
    }

    public static AuthenticationScheme oauth2(String accessToken, OAuthSignature signature) {
        return impl().oauth2(accessToken, signature);
    }

    public static AuthenticationScheme digest(String userName, String password) {
        return impl().digest(userName, password);
    }

    public static Response post() {
        return impl().post();
    }

    public static Response delete(URL url) {
        return impl().delete(url);
    }

    public static Response get() {
        return impl().get();
    }

    public static Response patch(String path, Map<String, ?> pathParams) {
        return impl().patch(path, pathParams);
    }

    public static Response post(String path, Object... pathParams) {
        return impl().post(path, pathParams);
    }

    public static Response options(String path, Map<String, ?> pathParams) {
        return impl().options(path, pathParams);
    }

    public static PreemptiveAuthProvider preemptive() {
        return impl().preemptive();
    }

    public static void replaceFiltersWith(List<Filter> filters) {
        impl().replaceFiltersWith(filters);
    }

    public static List<Filter> filters() {
        return impl().filters();
    }

    public static Response options() {
        return impl().options();
    }

    public static Response patch(String path, Object... pathParams) {
        return impl().patch(path, pathParams);
    }

    public static Response post(URL url) {
        return impl().post(url);
    }

    public static void proxy(URI uri) {
        impl().proxy(uri);
    }

    public static void keyStore(String pathToJks, String password) {
        impl().keyStore(pathToJks, password);
    }

    public static void enableLoggingOfRequestAndResponseIfValidationFails(LogDetail logDetail) {
        impl().enableLoggingOfRequestAndResponseIfValidationFails(logDetail);
    }

    public static void registerParser(String contentType, Parser parser) {
        impl().registerParser(contentType, parser);
    }

    public static void objectMapper(ObjectMapper objectMapper) {
        impl().objectMapper(objectMapper);
    }

    public static void setBasePath(String basePath) {
        impl().setBasePath(basePath);
    }

    public static void reset() {
        impl().reset();
    }

    public static AuthenticationScheme basic(String userName, String password) {
        return impl().basic(userName, password);
    }

    public static Response delete() {
        return impl().delete();
    }

    public static AuthenticationScheme oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken, OAuthSignature signature) {
        return impl().oauth(consumerKey, consumerSecret, accessToken, secretToken, signature);
    }

    public static Response head(String path, Map<String, ?> pathParams) {
        return impl().head(path, pathParams);
    }

    public static Response options(URL url) {
        return impl().options(url);
    }

    public static Response post(String path, Map<String, ?> pathParams) {
        return impl().post(path, pathParams);
    }

    public static RequestSpecification with() {
        return impl().with();
    }

    public static Response get(String path, Object... pathParams) {
        return impl().get(path, pathParams);
    }

    public static List<Argument> withArgs(Object firstArgument, Object... additionalArguments) {
        return impl().withArgs(firstArgument, additionalArguments);
    }

    public static List<Argument> withNoArgs() {
        return impl().withNoArgs();
    }

    public static void filters(Filter filter, Filter... additionalFilters) {
        impl().filters(filter, additionalFilters);
    }

    public static void proxy(int port) {
        impl().proxy(port);
    }

    public static AuthenticationScheme oauth(String consumerKey, String consumerSecret, String accessToken, String secretToken) {
        return impl().oauth(consumerKey, consumerSecret, accessToken, secretToken);
    }

    public static ProxySpecification getProxy() {
        return impl().getProxy();
    }

    public static void setProxy(ProxySpecification proxy) {
        impl().setProxy(proxy);
    }

    public static String getSessionId() {
        return impl().getSessionId();
    }

    public static void setSessionId(String sessionId) {
        impl().setSessionId(sessionId);
    }

    public static ResponseSpecification getResponseSpecification() {
        return impl().getResponseSpecification();
    }

    public static void setResponseSpecification(ResponseSpecification responseSpecification) {
        impl().setResponseSpecification(responseSpecification);
    }

    public static RequestSpecification getRequestSpecification() {
        return impl().getRequestSpecification();
    }

    public static void setRequestSpecification(RequestSpecification requestSpecification) {
        impl().setRequestSpecification(requestSpecification);
    }

    public static boolean isUrlEncodingEnabled() {
        return impl().isUrlEncodingEnabled();
    }

    public static void setUrlEncodingEnabled(boolean urlEncodingEnabled) {
        impl().setUrlEncodingEnabled(urlEncodingEnabled);
    }

    public static RestAssuredConfig getConfig() {
        return impl().getConfig();
    }

    public static void setConfig(RestAssuredConfig config) {
        impl().setConfig(config);
    }

    public static String getRootPath() {
        return impl().getRootPath();
    }

    public static void setRootPath(String rootPath) {
        impl().setRootPath(rootPath);
    }

    public static Response request(Method method) {
        return impl().request(method);
    }

    public static Response request(String method) {
        return impl().request(method);
    }

    public static Response request(Method method, String path, Object... pathParams) {
        return impl().request(method, path, pathParams);
    }

    public static Response request(String method, String path, Object... pathParams) {
        return impl().request(method, path, pathParams);
    }

    public static Response request(Method method, URI uri) {
        return impl().request(method, uri);
    }

    public static Response request(Method method, URL url) {
        return impl().request(method, url);
    }

    public static Response request(String method, URI uri) {
        return impl().request(method, uri);
    }

    public static Response request(String method, URL url) {
        return impl().request(method, url);
    }

}
