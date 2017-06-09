package com.sparender;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
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

	private final ObjectPool<RemoteWebDriver> driverPool;

	public SeleniumRenderer() {

		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(POOL_MAX_SIZE);
		driverPool = new GenericObjectPool<>(new WebDriverFactory(), config);
	}

	@Override
	public String render(final String requestedUrl) throws Exception {

		RemoteWebDriver webDriver = null;

		try {

			final long start = System.currentTimeMillis();

			LOGGER.info("Trying to borrow a driver from the pool for ..." + requestedUrl);
			webDriver = driverPool.borrowObject();
			LOGGER.info("Got the web driver " + webDriver.getSessionId());

			webDriver.get(requestedUrl);

			sleep(TIME_TO_WAIT_FOR_RENDER);

			LOGGER.info("Selenium finished rendering " + requestedUrl + " in " + (System.currentTimeMillis() - start) + " ms for web driver " + webDriver.getSessionId());

			String source = webDriver.getPageSource();

			LOGGER.info("Got page source for " + requestedUrl + " in " + (System.currentTimeMillis() - start) + " ms for web driver " + webDriver.getSessionId());
			String content = updatePageSource(requestedUrl, source, "https://www.nextprot.org");

			LOGGER.info("Updating page source in " + (System.currentTimeMillis() - start) + " ms and returning web driver " + webDriver.getSessionId() + " to the pool");
			driverPool.returnObject(webDriver);

			return content;
		} catch (Exception e) {

			if (webDriver != null) {

				LOGGER.error("Session " + webDriver.getSessionId() + " died: " + e.getMessage());
				driverPool.invalidateObject(webDriver);

				try {
					webDriver.close();
					webDriver.quit();
				} catch (Exception e2) {
					LOGGER.error("Fails to properly close session " + webDriver.getSessionId() + ": " + e2.getMessage());
				}
				return render(requestedUrl);
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
					sb.append(css + "\n");
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
			System.err.println(seoUrl);
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
}
