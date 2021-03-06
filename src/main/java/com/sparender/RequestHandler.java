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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RequestHandler extends AbstractHandler implements Handler {

	final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

	private final Renderer renderer;
	private final RequestLogger logger;
	private final ContentCache cache;
	private final Boolean cacheEnabled;
	private final RunningRequests runningRequests;
	private Map<String, Object> lockedUrls;

	public RequestHandler() {
		runningRequests = new RunningRequests();
		cache = new ContentCache();
		renderer = new SeleniumRenderer(); 
		logger = new RequestLogger(runningRequests);
		cacheEnabled = Boolean.valueOf(App.prop.get("cache.enabled"));
		LOGGER.info("Cache " + ((cacheEnabled) ? "enabled" : "disabled") );
		lockedUrls = Collections.synchronizedMap(new HashMap<>());

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

		if (request.getRequestURI().equals("/requests")) {
			response.setStatus(HttpServletResponse.SC_OK);
			out.println(runningRequests.getRunningRequests());
			baseRequest.setHandled(true);
			return;
		}

		final String requestUrl = Utils.getFullURL(request).substring(1).replace("?_escaped_fragment_=", "").replace("&_escaped_fragment_=", "");

		if (!requestUrl.startsWith("http")) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			if (!requestUrl.startsWith("favicon.ico")) {
				out.println("Expecting a URL starting with http at this stage, got:" + requestUrl);
				LOGGER.info("Responding with bad request for " + requestUrl);
			}

			baseRequest.setHandled(true);
			return;

		} else {

			logger.logBefore(request, requestUrl);

			synchronized (getLockingObject(requestUrl)){

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


	private synchronized Object getLockingObject(String requestedUrl) {

		//TODO evaluate the memory of this. Do we need to run a recycling here?
		if(!lockedUrls.containsKey(requestedUrl)){
			lockedUrls.put(requestedUrl, new Object());
		}

		return lockedUrls.get(requestedUrl);
	}


}