package com.sparender;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class RequestHandler extends AbstractHandler implements Handler {

	final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

	private final Renderer renderer;
	private final RequestLogger logger;
	private final ContentCache cache;
	private final Boolean cacheEnabled;

	public RequestHandler() {
		cache = new ContentCache();
		renderer = new SeleniumRenderer(); // new SeleniumRendererProto();
		logger = new RequestLogger();
		cacheEnabled = Boolean.valueOf(App.prop.get("cache.enabled"));
		LOGGER.info("Cache " + ((cacheEnabled) ? "enabled" : "disabled") );
	}

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		boolean cacheHit = true;
		long start = System.currentTimeMillis();
		String errorMessage = null;
		response.setContentType("text/html; charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);

		PrintWriter out = response.getWriter();
		String content = null;

		final String requestUrl = getFullURL(request).substring(1)
				.replace("?_escaped_fragment_=", "")
				.replace("&_escaped_fragment_=", "")
				;

		logger.logBefore(request, requestUrl);

		if (!requestUrl.startsWith("http")) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			if (!requestUrl.startsWith("favicon.ico")) {
				out.println("Expecting a URL starting with http at this stage, got:" + requestUrl);
				LOGGER.info("Responding with bad request for " + requestUrl);
			}

			baseRequest.setHandled(true);
			return;

		} else {

			try {
				if (!cacheEnabled || !cache.contentExists(requestUrl)) {
					LOGGER.info("Requesting Selenium to render page " + requestUrl);
					content = renderer.render(requestUrl);
					cache.putContent(requestUrl, content);
					cacheHit = false;
				} else {
					LOGGER.info("Hitting the cache for page " + requestUrl);
					content = cache.getContent(requestUrl);
				}
			} catch (Exception e) {
				cacheHit = false;
				e.printStackTrace();
				errorMessage = e.getMessage();
				response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				out.println("Failed to render " + requestUrl + " " + e.getMessage());
			}

		}

		Integer contentLength = 0;
		Integer bytes = 0;

		if (content != null) {
			out.println(content);
			contentLength = content.length();
			bytes = content.getBytes("UTF-8").length;
		}

		logger.logAfter(request, response, requestUrl, contentLength, bytes, cacheHit, start, errorMessage);

		baseRequest.setHandled(true);
	}

	private static String getFullURL(HttpServletRequest request) {
		String requestURL = request.getPathInfo();
		String queryString = request.getQueryString();

		if (queryString == null) {
			return requestURL.toString();
		} else {
			return requestURL + '?' + queryString;
		}
	}
}