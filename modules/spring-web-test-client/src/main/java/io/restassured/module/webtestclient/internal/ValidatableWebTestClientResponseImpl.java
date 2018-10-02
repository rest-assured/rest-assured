package io.restassured.module.webtestclient.internal;

import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.ValidatableResponseOptionsImpl;
import io.restassured.internal.assertion.AssertParameter;
import io.restassured.internal.log.LogRepository;
import io.restassured.module.webtestclient.response.ValidatableWebTestClientResponse;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.response.ExtractableResponse;

import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.ResultActions;

import static io.restassured.module.webtestclient.internal.ResponseConverter.toStandardResponse;

/**
 * @author Olga Maciaszek-Sharma
 */
public class ValidatableWebTestClientResponseImpl extends ValidatableResponseOptionsImpl<ValidatableWebTestClientResponse, WebTestClientResponse> implements ValidatableWebTestClientResponse {

	private final WebTestClient.ResponseSpec responseSpec;
	private final WebTestClientResponse response;

	public ValidatableWebTestClientResponseImpl(WebTestClient.ResponseSpec responseSpec, ResponseParserRegistrar rpr,
	                                            RestAssuredConfig config,
	                                            WebTestClientResponse response,
	                                            ExtractableResponse<WebTestClientResponse> extractableResponse,
	                                            LogRepository logRepository) {
		super(rpr, config, toStandardResponse(response), extractableResponse, logRepository);
		this.response = response;
		AssertParameter.notNull(responseSpec, ResultActions.class);
		this.responseSpec = responseSpec;
	}

	@Override
	public WebTestClientResponse originalResponse() {
		return response;
	}

	@Override
	public WebTestClient.ResponseSpec responseSpec() {
		return responseSpec;
	}

	@Override
	public WebTestClient.ResponseSpec expect() {
		return responseSpec();
	}
}
