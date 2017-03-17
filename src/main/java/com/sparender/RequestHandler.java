package com.sparender;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler extends AbstractHandler implements Handler {

	final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

	private SeleniumRenderer seleniumRenderer;
	private RequestLogger logger;
	private ContentCache cache;

	public RequestHandler() {
		cache = new ContentCache();
		seleniumRenderer = new SeleniumRenderer(cache);
		logger = new RequestLogger();
	}

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		boolean cacheHit = false;
		long start = System.currentTimeMillis();
		response.setContentType("text/html; charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		
		PrintWriter out = response.getWriter();
		Optional<String> contentString = Optional.empty();
		
		final String requestUrl = getFullURL(request).substring(1).replace("?_escaped_fragment_=", "");

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

				if (cache.contentExists(requestUrl)) {
					cacheHit = true;
					contentString = Optional.of(cache.getContent(requestUrl));
				} else {
					Future<String> content = seleniumRenderer.startRendering(requestUrl);
					contentString = Optional.of(content.get());
				}

			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				out.println("Failed to render " + requestUrl + " " + e.getMessage());
			}

		}
		
		Integer contentLength = 0;
		Integer bytes = 0;
		
		if(contentString.isPresent()){
			out.println(contentString.get());
			contentLength = contentString.get().length();
			bytes = contentString.get().getBytes("UTF-8").length;
		}

		logger.log(request, response, requestUrl, contentLength, bytes, cacheHit, start);

		baseRequest.setHandled(true);
	}
	
	
	private static String getFullURL(HttpServletRequest request) {
	    String requestURL = request.getPathInfo();
	    String queryString = request.getQueryString();

	    if (queryString == null) {
	        return requestURL.toString();
	    } else {
	        return requestURL + '?'  + queryString;
	    }
	}
}