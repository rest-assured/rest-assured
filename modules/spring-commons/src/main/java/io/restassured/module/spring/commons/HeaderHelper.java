package io.restassured.module.spring.commons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.restassured.config.HeaderConfig;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.module.spring.commons.config.SpecificationConfig;

import static io.restassured.internal.assertion.AssertParameter.notNull;

/**
 * @author Olga Maciaszek-Sharma
 */
public class HeaderHelper {

	private static final String CONTENT_TYPE = "Content-Type";

	private HeaderHelper() {
	}

	public static Headers headers(Headers requestHeaders, Map<String, ?> headers, SpecificationConfig config) {
		notNull(headers, "headers");
		List<Header> headerList = new ArrayList<Header>();
		if (requestHeaders.exist()) {
			for (Header requestHeader : requestHeaders) {
				headerList.add(requestHeader);
			}
		}

		for (Map.Entry<String, ?> stringEntry : headers.entrySet()) {
			Object value = stringEntry.getValue();
			if (value instanceof List) {
				List<?> values = (List<?>) value;
				for (Object headerValue : values) {
					headerList.add(new Header(stringEntry.getKey(), Serializer.serializeIfNeeded(headerValue,
							getRequestContentType(requestHeaders), config)));
				}
			} else {
				headerList.add(new Header(stringEntry.getKey(), Serializer.serializeIfNeeded(value,
						getRequestContentType(requestHeaders), config)));
			}
		}
		return new Headers(headerList);
	}

	public static String getRequestContentType(Headers requestHeaders) {
		Header header = requestHeaders.get(CONTENT_TYPE);
		if (header != null) {
			return header.getValue();
		}
		return null;
	}

	public static Headers headers(Headers requestHeaders, Headers headersToAdd, HeaderConfig headerConfig) {
		notNull(headersToAdd, "Headers");
		if (headersToAdd.exist()) {
			List<Header> headerList = new ArrayList<Header>();
			if (requestHeaders.exist()) {
				for (Header requestHeader : requestHeaders) {
					headerList.add(requestHeader);
				}
			}

			for (Header requestHeader : headersToAdd) {
				headerList.add(requestHeader);
			}
			return new Headers(removeMergedHeadersIfNeeded(headerList, headerConfig));
		}
		return requestHeaders;
	}

	private static List<Header> removeMergedHeadersIfNeeded(List<Header> headerList, HeaderConfig headerConfig) {
		List<Header> filteredList = new ArrayList<Header>();
		for (Header header : headerList) {
			String headerName = header.getName();
			if (headerConfig.shouldOverwriteHeaderWithName(headerName)) {
				int index = -1;
				for (int i = 0; i < filteredList.size(); i++) {
					Header filteredHeader = filteredList.get(i);
					if (filteredHeader.hasSameNameAs(header)) {
						index = i;
						break;
					}
				}

				if (index != -1) {
					filteredList.remove(index);
				}
			}

			filteredList.add(header);
		}
		return filteredList;
	}

	public static Headers headers(final Headers requestHeaders, final String headerName, final Object headerValue,
	                              final SpecificationConfig config,
	                              Object... additionalHeaderValues) {
		notNull(headerName, "Header name");
		notNull(headerValue, "Header value");

		List<Header> headerList = new ArrayList<Header>() {{
			add(new Header(headerName, Serializer.serializeIfNeeded(headerValue,
					getRequestContentType(requestHeaders), config)));
		}};

		if (additionalHeaderValues != null) {
			for (Object additionalHeaderValue : additionalHeaderValues) {
				headerList.add(new Header(headerName, Serializer.serializeIfNeeded(additionalHeaderValue,
						getRequestContentType(requestHeaders), config)));
			}
		}
		return new Headers(headerList);
	}

}
