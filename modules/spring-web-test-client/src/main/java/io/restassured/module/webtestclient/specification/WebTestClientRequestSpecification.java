package io.restassured.module.webtestclient.specification;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import io.restassured.http.Method;
import io.restassured.module.webtestclient.response.WebTestClientResponse;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WebTestClientRequestSpecification implements WebTestClientRequestSender {

	@Override
	public WebTestClientResponse get(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse get(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse post(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse post(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse put(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse put(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse delete(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse delete(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse head(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse head(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse patch(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse patch(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse options(String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse options(String path, Map<String, ?> pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse get(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse post(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse put(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse delete(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse head(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse patch(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse options(URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse get(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse post(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse put(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse delete(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse head(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse patch(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse options(URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse get() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse post() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse put() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse delete() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse head() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse patch() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse options() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(Method method) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(String method) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(Method method, String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(String method, String path, Object... pathParams) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(Method method, URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(Method method, URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(String method, URI uri) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientResponse request(String method, URL url) {
		throw new UnsupportedOperationException("Please, implement me.");
	}
}
