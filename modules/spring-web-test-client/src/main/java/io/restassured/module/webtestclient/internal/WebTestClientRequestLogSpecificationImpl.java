package io.restassured.module.webtestclient.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;

import io.restassured.authentication.NoAuthScheme;
import io.restassured.filter.Filter;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.internal.LogSpecificationImpl;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.module.webtestclient.specification.WebTestClientRequestLogSpecification;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import io.restassured.specification.FilterableRequestSpecification;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WebTestClientRequestLogSpecificationImpl extends LogSpecificationImpl implements WebTestClientRequestLogSpecification {

	private final WebTestClientRequestSpecificationImpl requestSpecification;

	public WebTestClientRequestLogSpecificationImpl(WebTestClientRequestSpecificationImpl requestSpecification) {
		this.requestSpecification = requestSpecification;
	}

	@Override
	public WebTestClientRequestSpecification body() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification body(boolean shouldPrettyPrint) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification all() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification all(boolean shouldPrettyPrint) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification everything() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification everything(boolean shouldPrettyPrint) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification headers() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification cookies() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification ifValidationFails() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification ifValidationFails(LogDetail logDetail) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		requestSpecification.getLogRepository().registerRequestLog(baos);
		return logWith(logDetail, shouldPrettyPrint, ps);
	}

	private WebTestClientRequestSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled,
	                                                  PrintStream printStream) {
		boolean shouldUrlEncodeRequestUri = requestSpecification.getRestAssuredWebTestClientConfig()
				.getLogConfig().shouldUrlEncodeRequestUri();
		requestSpecification.setRequestLoggingFilter(new RequestLoggingFilter(logDetail, prettyPrintingEnabled, printStream, shouldUrlEncodeRequestUri));
		return requestSpecification;
	}

	// TODO
	private FilterableRequestSpecification toFilterableRequestSpecification(WebTestClientRequestSpecification requestSpecification) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	private WebTestClientRequestSpecification logWith(LogDetail logDetail) {
		RequestSpecificationImpl reqSpec = toRequestSpecification();
		return logWith(logDetail, shouldPrettyPrint(reqSpec));
	}

	// FIXME
	private RequestSpecificationImpl toRequestSpecification() {
		return new RequestSpecificationImpl("", 8080, "", new NoAuthScheme(), Collections.<Filter>emptyList(), null, true, requestSpecification.getRestAssuredConfig(), requestSpecification.getLogRepository(), null
		);
	}

	private WebTestClientRequestSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled) {
		return logWith(logDetail, prettyPrintingEnabled, getPrintStream(toRequestSpecification()));
	}

}
