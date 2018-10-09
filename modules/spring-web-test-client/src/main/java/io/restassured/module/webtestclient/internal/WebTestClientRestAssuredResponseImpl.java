package io.restassured.module.webtestclient.internal;

import io.restassured.config.LogConfig;
import io.restassured.internal.RestAssuredResponseOptionsImpl;
import io.restassured.internal.log.LogRepository;
import io.restassured.module.webtestclient.response.ValidatableWebTestClientResponse;
import io.restassured.module.webtestclient.response.WebTestClientResponse;

import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WebTestClientRestAssuredResponseImpl extends RestAssuredResponseOptionsImpl<WebTestClientResponse> implements WebTestClientResponse {

	// FIXME: final
	private WebTestClient.ResponseSpec responseSpec;
	private final LogRepository logRepository;

	public WebTestClientRestAssuredResponseImpl(WebTestClient.ResponseSpec responseSpec,
	                                            LogRepository logRepository) {
		this.responseSpec = responseSpec;
		this.logRepository = logRepository;
	}

	@Override
	public ValidatableWebTestClientResponse then() {
		ValidatableWebTestClientResponse response = new ValidatableWebTestClientResponseImpl(
				responseSpec, getRpr(), getConfig(), this, this, logRepository);
		LogConfig logConfig = getConfig().getLogConfig();
		if (logConfig.isLoggingOfRequestAndResponseIfValidationFailsEnabled()) {
			response.log().ifValidationFails(logConfig.logDetailOfRequestAndResponseIfValidationFails(), logConfig.isPrettyPrintingEnabled());
		}
		return response;
	}

	public FluxExchangeResult<Object> getWebTestClientResult() {
		return webTestClientResult();
	}

	public FluxExchangeResult<Object> webTestClientResult() {
		return responseSpec.returnResult(Object.class);
	}

	public void setResponseSpec(WebTestClient.ResponseSpec responseSpec) {
		this.responseSpec = responseSpec;
	}
}
