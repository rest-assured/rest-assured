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
		return body(shouldPrettyPrint(toRequestSpecification()));
	}

	@Override
	public WebTestClientRequestSpecification body(boolean shouldPrettyPrint) {
		return logWith(LogDetail.BODY, shouldPrettyPrint);
	}

	@Override
	public WebTestClientRequestSpecification all() {
		return all(shouldPrettyPrint(toRequestSpecification()));
	}

	@Override
	public WebTestClientRequestSpecification all(boolean shouldPrettyPrint) {
		return logWith(LogDetail.ALL, shouldPrettyPrint);
	}

	@Override
	public WebTestClientRequestSpecification everything() {
		return all();
	}

	@Override
	public WebTestClientRequestSpecification everything(boolean shouldPrettyPrint) {
		return all(shouldPrettyPrint);
	}

	@Override
	public WebTestClientRequestSpecification headers() {
		return logWith(LogDetail.HEADERS);
	}

	@Override
	public WebTestClientRequestSpecification cookies() {
		return logWith(LogDetail.COOKIES);
	}

	@Override
	public WebTestClientRequestSpecification ifValidationFails() {
		return ifValidationFails(LogDetail.ALL);
	}

	@Override
	public WebTestClientRequestSpecification ifValidationFails(LogDetail logDetail) {
		return ifValidationFails(logDetail, shouldPrettyPrint(toRequestSpecification()));
	}

	@Override
	public WebTestClientRequestSpecification ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(byteArrayOutputStream);
		requestSpecification.getLogRepository().registerRequestLog(byteArrayOutputStream);
		return logWith(logDetail, shouldPrettyPrint, printStream);
	}

	private WebTestClientRequestSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled,
	                                                  PrintStream printStream) {
		boolean shouldUrlEncodeRequestUri = requestSpecification.getRestAssuredWebTestClientConfig()
				.getLogConfig().shouldUrlEncodeRequestUri();
		requestSpecification.setRequestLoggingFilter(new RequestLoggingFilter(logDetail, prettyPrintingEnabled, printStream, shouldUrlEncodeRequestUri));
		return requestSpecification;
	}

	private WebTestClientRequestSpecification logWith(LogDetail logDetail) {
		RequestSpecificationImpl reqSpec = toRequestSpecification();
		return logWith(logDetail, shouldPrettyPrint(reqSpec));
	}

	public WebTestClientRequestSpecification params() {
		return logWith(LogDetail.PARAMS);
	}

	public WebTestClientRequestSpecification parameters() {
		return logWith(LogDetail.PARAMS);
	}

	private RequestSpecificationImpl toRequestSpecification() {
		return new RequestSpecificationImpl("", 8080, "", new NoAuthScheme(),
				Collections.<Filter>emptyList(), null, true,
				requestSpecification.getRestAssuredConfig(), requestSpecification.getLogRepository(), null
		);
	}

	private WebTestClientRequestSpecification logWith(LogDetail logDetail, boolean prettyPrintingEnabled) {
		return logWith(logDetail, prettyPrintingEnabled, getPrintStream(toRequestSpecification()));
	}

}
