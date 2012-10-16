package org.springone.push;

import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

public class CorsHandler implements Handler<HttpServerRequest> {

	private static final String LOCATION = "Location";
	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "access-control-allow-origin";
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "access-control-allow-methods";
	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "access-control-allow-headers";
	private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "access-control-expose-headers";
	private static final String ACCESS_CONTROL_REQUEST_METHOD = "access-control-request-method";
	private static final String ACCESS_CONTROL_REQUEST_HEADERS = "access-control-request-headers";
	private static final String ACCESS_CONTROL_MAX_AGE = "access-control-max-age";
	private static final String CACHE_SECONDS = "300";

	private final Handler<HttpServerRequest> next;

	public CorsHandler(Handler<HttpServerRequest> next) {
		this.next = next;
	}

	public void handle(HttpServerRequest request) {
		System.out.println("Request intercepted: " + request.path);
		
		HttpServerResponse response = request.response;

		// For PUT requests we need an extra round-trip
		// See e.g. http://www.html5rocks.com/en/tutorials/cors/

		String acRequestMethod = request.headers().get(ACCESS_CONTROL_REQUEST_METHOD);
		String acRequestHeaders = request.headers().get(ACCESS_CONTROL_REQUEST_HEADERS);

		// Our REST API is accessible from anywhere
		response.putHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");

		if (HttpMethod.OPTIONS.toString().equals(request.method)
				&& StringUtils.hasText(acRequestMethod)) {
			// this is a preflight check
			// our API only needs this for PUT requests, anything we can PUT we
			// can also GET
			response.putHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT");
			response.putHeader(ACCESS_CONTROL_ALLOW_HEADERS, acRequestHeaders);
			response.putHeader(ACCESS_CONTROL_MAX_AGE, CACHE_SECONDS);

			response.end();
			return;
		} else {
			response.putHeader(ACCESS_CONTROL_EXPOSE_HEADERS, LOCATION);
		}

		// Not a preflight check, continue as normal
		this.next.handle(request);
	}

}
