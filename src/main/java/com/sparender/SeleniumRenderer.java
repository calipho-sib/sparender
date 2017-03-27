package com.sparender;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple HTTP Server that renders HTML pages using Selenium.
 * 
 */
public class SeleniumRenderer {

	private static final Integer TIME_TO_WAIT_FOR_RENDER = 2000;
	private static final int POOL_MAX_SIZE = Integer.parseInt(App.prop.get("driver.pool.max"));
	private static final String BASE_URL = App.prop.get("base.url");
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogger.class);

	private final ObjectPool<RemoteWebDriver> driverPool;

	public SeleniumRenderer() {
		driverPool = buildDriverPool();
	}

	public String render(final String requestedUrl) throws Exception {
		return render(requestedUrl, 0);
	}

	private GenericObjectPool<RemoteWebDriver> buildDriverPool() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(POOL_MAX_SIZE);
		return new GenericObjectPool<>(new WebDriverFactory(), config);
	}

	public String render(final String requestedUrl, int cnt) throws Exception {

		if (cnt > 0) {

			if (cnt > 10) {
				LOGGER.error("Failed to render the page " + requestedUrl);
				throw new RuntimeException("Failed to render the page" + requestedUrl);
			}

			LOGGER.warn("Trying again for " + requestedUrl + " waiting " + cnt + " seconds");
			sleep(cnt * 1000);
		}

		RemoteWebDriver webDriver = null;

		try {
			final long start = System.currentTimeMillis();

			LOGGER.info("Trying to borrow a driver from the pool...");
			webDriver = driverPool.borrowObject();
			LOGGER.info("Got the web driver " + webDriver.getSessionId());

			webDriver.get(requestedUrl);

			sleep(TIME_TO_WAIT_FOR_RENDER);

			LOGGER.info("Selenium finished rendering " + requestedUrl + " in " + (System.currentTimeMillis() - start)
					+ " ms");
			String content = updatePageSource(webDriver.getPageSource());

			return content;
		} catch (Exception e) {

			if (webDriver != null) {

				LOGGER.warn("Session " + webDriver.getSessionId() + " died: " + e.getMessage());
				driverPool.invalidateObject(webDriver);

				try {
					webDriver.close();
					webDriver.quit();
				} catch (Exception e2) {
					LOGGER.warn("Fails to properly close session " + webDriver.getSessionId() + ": " + e2.getMessage());
				} finally {
					webDriver = null;
				}
				return render(requestedUrl, cnt++);
			}

			throw e;
		} finally {

			if (null != webDriver) {
				LOGGER.info("Returning driver " + webDriver.getSessionId() + " to the pool");
				driverPool.returnObject(webDriver);
			}

		}

	}

	private static String updatePageSource(String content) {

		String contentWithoutJs = content.replaceAll("<script(.|\n)*?</script>", "");
		String contentWithoutJsAndHtmlImport = contentWithoutJs.replaceAll("<link rel=\"import\".*/>", "");
		String contentWithoutJsAndHtmlImportAndIframes = contentWithoutJsAndHtmlImport.replaceAll("<iframe .*</iframe>",
				"");
		return contentWithoutJsAndHtmlImportAndIframes.replaceAll("(<base.*?>)", "<base href=\"" + BASE_URL + "\"/>");
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
