package io.restassured.module.webtestclient.internal;

import java.util.function.Consumer;
import java.util.function.Function;

import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.ValidatableResponseOptionsImpl;
import io.restassured.internal.assertion.AssertParameter;
import io.restassured.internal.log.LogRepository;
import io.restassured.module.webtestclient.response.ValidatableWebTestClientResponse;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.response.ExtractableResponse;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import reactor.core.publisher.Flux;

import org.springframework.test.util.AssertionErrors;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static io.restassured.module.webtestclient.internal.ResponseConverter.toStandardResponse;

/**
 * @author Olga Maciaszek-Sharma
 */
public class ValidatableWebTestClientResponseImpl extends ValidatableResponseOptionsImpl<ValidatableWebTestClientResponse, WebTestClientResponse> implements ValidatableWebTestClientResponse {

	private final WebTestClientResponse response;
	private final FluxExchangeResult<String> result;

	public ValidatableWebTestClientResponseImpl(WebTestClient.ResponseSpec responseSpec,
	                                            ResponseParserRegistrar rpr,
	                                            RestAssuredConfig config,
	                                            WebTestClientResponse response,
	                                            ExtractableResponse<WebTestClientResponse> extractableResponse,
	                                            LogRepository logRepository) {
		super(rpr, config, toStandardResponse(response), extractableResponse, logRepository);
		AssertParameter.notNull(responseSpec, WebTestClient.ResponseSpec.class);
		result = responseSpec.returnResult(String.class);
		this.response = response;
	}

	@Override
	public WebTestClientResponse originalResponse() {
		return response;
	}

	@Override
	public ValidatableWebTestClientResponse assertThatBody(Matcher matcher) {
		return expectBody(matcher);
	}

	/**
	 * Copied from {@link org.springframework.test.web.reactive.server.DefaultWebTestClient} to allow using within
	 * RestAssured fluent API
	 */
	@Override
	public ValidatableWebTestClientResponse expectBody(Matcher matcher) {
		result.assertWithDiagnostics(() -> MatcherAssert.assertThat(result.getResponseBody(), matcher));
		return this;
	}

	/**
	 * Copied from {@link org.springframework.test.web.reactive.server.DefaultWebTestClient} to allow using within
	 * RestAssured fluent API
	 */
	@Override
	public ValidatableWebTestClientResponse isEqualTo(Object expected) {
		this.result.assertWithDiagnostics(() ->
				AssertionErrors.assertEquals("Response body", expected, this.result.getResponseBody()));
		return this;
	}

	@Override
	public ValidatableWebTestClientResponse expectHeaders(Matcher matcher) {
		result.assertWithDiagnostics(() -> MatcherAssert.assertThat(result.getResponseHeaders(), matcher));
		return this;
	}

	/**
	 * Copied from {@link org.springframework.test.web.reactive.server.DefaultWebTestClient} and adjusted to allow using
	 * within RestAssured fluent API
	 */
	@Override
	public <R> ValidatableWebTestClientResponse expectBody(Function<Flux, R> bodyMapper, Matcher<R> matcher) {
		result.assertWithDiagnostics(() -> {
			Flux body = result.getResponseBody();
			MatcherAssert.assertThat(bodyMapper.apply(body), matcher);
		});
		return this;
	}

	/**
	 * Copied from {@link org.springframework.test.web.reactive.server.DefaultWebTestClient}  and adjusted to allow using
	 * within RestAssured fluent API
	 */
	@Override
	public ValidatableWebTestClientResponse expectBody(Consumer consumer) {
		result.assertWithDiagnostics(() -> consumer.accept(this.result.getResponseBody()));
		return this;
	}
}
