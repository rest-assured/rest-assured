package io.restassured.module.webtestclient.internal;

import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.response.Response;

/**
 * @author Olga Maciaszek-Sharma
 */
class ResponseConverter {

	private ResponseConverter() {
	}

	static Response toStandardResponse(WebTestClientResponse response) {
		if (!(response instanceof WebTestClientRestAssuredResponseImpl)) {
			throw new IllegalArgumentException(WebTestClientResponse.class.getName() + " must be an instance of " + WebTestClientRestAssuredResponseImpl.class.getName());
		}
		WebTestClientRestAssuredResponseImpl webTestClientRestAssuredResponse = (WebTestClientRestAssuredResponseImpl) response;

		RestAssuredResponseImpl std = new RestAssuredResponseImpl();
		std.setConnectionManager(webTestClientRestAssuredResponse.getConnectionManager());
		std.setContent(webTestClientRestAssuredResponse.getContent());
		std.setContentType(webTestClientRestAssuredResponse.getContentType());
		std.setCookies(webTestClientRestAssuredResponse.detailedCookies());
		std.setDecoderConfig(webTestClientRestAssuredResponse.getDecoderConfig());
		std.setDefaultContentType(webTestClientRestAssuredResponse.getDefaultContentType());
		std.setHasExpectations(webTestClientRestAssuredResponse.getHasExpectations());
		std.setResponseHeaders(webTestClientRestAssuredResponse.getResponseHeaders());
		std.setSessionIdName(webTestClientRestAssuredResponse.getSessionIdName());
		std.setStatusCode(webTestClientRestAssuredResponse.getStatusCode());
		std.setStatusLine(webTestClientRestAssuredResponse.getStatusLine());
		std.setRpr(webTestClientRestAssuredResponse.getRpr());
		std.setFilterContextProperties(webTestClientRestAssuredResponse.getFilterContextProperties());
		std.setLogRepository(webTestClientRestAssuredResponse.getLogRepository());
		return std;
	}

}
