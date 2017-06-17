package com.sparender;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.corba.se.spi.ior.ObjectKey;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple HTTP Server that renders HTML pages using Selenium.
 * 
 */
public class SeleniumRenderer implements Renderer {

	private static final Integer TIME_TO_WAIT_FOR_RENDER = 5000;
	private static final int POOL_MAX_SIZE = Integer.parseInt(App.prop.get("driver.pool.max"));
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogger.class);
	private final ExecutorService timeoutExecutorService = Executors.newFixedThreadPool(POOL_MAX_SIZE);

	private final ObjectPool<RemoteWebDriver> driverPool;

	public SeleniumRenderer() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(POOL_MAX_SIZE);
		driverPool = new GenericObjectPool<>(new WebDriverFactory(), config);
	}

	public String render(final String requestedUrl, int attemptCount) throws Exception {

		RemoteWebDriver webDriver = null;

		if(attemptCount > 3){
			LOGGER.error("Total disaster:  Reached the maximum number of attempts for " + requestedUrl);
			throw new RuntimeException("Total disaster:  Reached the maximum attempts for " + requestedUrl);
		}

		try {

			LOGGER.info("Trying to borrow a driver from the pool for ..." + requestedUrl + " attempt: " + attemptCount);
			webDriver = driverPool.borrowObject();
			LOGGER.info("Got the web driver " + webDriver.getSessionId());

			Callable<String> c = new TimeoutRenderer(webDriver, requestedUrl);

			//Should get the result within the given time. Otherwise a timeout is thrown and the driver invalidated.
			Future<String> future = timeoutExecutorService.submit(c);
			String content = future.get(20, TimeUnit.MINUTES);

			driverPool.returnObject(webDriver);

			return content;


		} catch (Exception e) {

			if (webDriver != null) {

				LOGGER.error("Session " + webDriver.getSessionId() + " died or was timeout : " + ExceptionUtils.getStackTrace(e));
				driverPool.invalidateObject(webDriver);

				try {
					webDriver.quit();
				} catch (Exception e2) {
					LOGGER.error("Fails to properly destroy session " + webDriver.getSessionId() + ": " + ExceptionUtils.getStackTrace(e));
				}
				return render(requestedUrl, attemptCount + 1);
			}

			throw e;
		}
	}

	private static String updatePageSource(String requestedUrl, String content, String baseUrl) {

		content = content.replaceAll("<script[^¿]+?<\\/script>", "");

		String cssRegex = "<style[^¿]+?<\\/style>";

		String css = getAllCss(content, cssRegex);

		content = content.replaceAll(cssRegex, "");

		// Remove all comments
		content = content.replaceAll("<!--[^¿]+?-->", "");

		// Remove all iframes
		content = content.replaceAll("<link rel=\"import\".*/>", "");

		// Replace with good base
		content = content.replaceAll("(<base.*?>)", "<base href=\"" + baseUrl + "\"/>");

		// Put the css at the end
		String jsonLd = getJSONLD(requestedUrl);
		
		// Remove empty lines, add json ld and css
		StringBuffer sb = new StringBuffer();
		String[] lines = content.split("\n");
		Arrays.stream(lines).forEach(l -> {
			if (!l.trim().isEmpty()){
				if(l.contains("</head>")){ //Before head tag include json ld
					sb.append(jsonLd + "\n");
				}
				//Append the line
				sb.append(l + "\n");
				if(l.contains("<header>")){ //Aftre header tag include the css
					//Not adding css for now
					//sb.append(css + "\n");
				}

			}
		});

		return sb.toString();
	}

	private static String getAllCss(String content, String cssRegex) {
		StringBuffer sb = new StringBuffer();
		Pattern p = Pattern.compile(cssRegex);

		Matcher matcher = p.matcher(content);
		while (matcher.find()) {
			String match = matcher.group();
			sb.append(match);
		}

		return sb.toString().replaceAll("\\s+", " ");
	}

	// From here:
	// http://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java

	static String getJSONLD(String url) {
		ObjectMapper mapper = new ObjectMapper();
		SeoTags tags;
		try {
			URL u = new URL(url);
			
			String seoUrl = "https://api.nextprot.org/seo/tags" + u.getPath();
			tags = mapper.readValue(new URL(seoUrl), SeoTags.class);
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}

		tags.getMetaDescription();
		tags.getTitle();
		
		StructuredJSONLDData jsonld = new StructuredJSONLDData("WebPage", url, tags.getH1(), tags.getMetaDescription());

		return jsonld.toString();
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String render(String requestedUrl) throws Exception {
		return render(requestedUrl, 1);
	}

	private static class TimeoutRenderer implements Callable {

		private String requestedUrl;
		private RemoteWebDriver webDriver;

		TimeoutRenderer(RemoteWebDriver webDriver, String url){
			this.requestedUrl = url;
			this.webDriver = webDriver;
		}

		@Override
		public String call() throws Exception {

			final long start = System.currentTimeMillis();

			LOGGER.info("Initializing the rendering process in another thread for " + requestedUrl + " in " + (System.currentTimeMillis() - start) + " ms for web driver " + webDriver.getSessionId());

			webDriver.get(requestedUrl);

			sleep(TIME_TO_WAIT_FOR_RENDER);

			LOGGER.info("Selenium finished rendering " + requestedUrl + " in " + (System.currentTimeMillis() - start) + " ms for web driver " + webDriver.getSessionId());

			String source = webDriver.getPageSource();

			LOGGER.info("Got page source for " + requestedUrl + " in " + (System.currentTimeMillis() - start) + " ms for web driver " + webDriver.getSessionId());
			String content = updatePageSource(requestedUrl, source, "https://www.nextprot.org");

			LOGGER.info("Updating page source in " + (System.currentTimeMillis() - start) + " ms and returning web driver " + webDriver.getSessionId() + " to the pool");

			return content;
		}
	}


}
