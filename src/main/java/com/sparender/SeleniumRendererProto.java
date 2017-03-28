package com.sparender;

import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.concurrent.ExecutorService;

/**
 * Simple HTTP Server that renders HTML pages using Selenium.
 * 
 */
public class SeleniumRendererProto implements Renderer {

	//private static final Integer TIME_TO_WAIT_FOR_RENDER = 2000;
	private static final int POOL_MAX_SIZE = Integer.parseInt(App.prop.get("driver.pool.max"));
	private static final String BASE_URL = App.prop.get("base.url");
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogger.class);

	private static ExecutorService executor;

	static final String SELENIUM_URL = App.prop.get("selenium.url");

	// private final ObjectPool<RemoteWebDriver> driverPool;

	public SeleniumRendererProto() {
		//executor = Executors.newFixedThreadPool(POOL_MAX_SIZE);
		// driverPool = buildDriverPool();
	}

	@Override
	public String render(final String requestedUrl) throws Exception {
		return render(requestedUrl, 0);
		/*try {

			return executor.submit(() -> {
				return render(requestedUrl, 0);
			}).get(10, TimeUnit.MINUTES);
			
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException("Timeout reached to render for " + requestedUrl);
		}*/
	}

	/*
	 * private GenericObjectPool<RemoteWebDriver> buildDriverPool() {
	 * GenericObjectPoolConfig config = new GenericObjectPoolConfig();
	 * config.setMaxTotal(POOL_MAX_SIZE); return new GenericObjectPool<>(new
	 * WebDriverFactory(), config); }
	 */

	private void tryToCloseWebDriver(RemoteWebDriver webDriver) {

		if (webDriver != null) {
			try {
				String sessionId = webDriver.getSessionId().toString();
				LOGGER.info("Trying to quit" +  sessionId);
				webDriver.quit();
				LOGGER.info("Web driver " +  sessionId + " sucessfully quitted");

			} catch (Exception e2) {
				LOGGER.error("Fails to properly quie driver with session " + webDriver.getSessionId() + ": "
						+ e2.getMessage());
			}

		}
	}

	public String render(final String requestedUrl, int cnt) throws Exception {

		if (cnt > 0) {

			if (cnt > 4) {
				LOGGER.error("Failed to render the page " + requestedUrl);
				throw new RuntimeException("Failed to render the page" + requestedUrl);
			}

			LOGGER.warn("Trying again for " + requestedUrl + " waiting " + cnt + " seconds");
			sleep(cnt * 1000);
		}

		RemoteWebDriver webDriver = null;

		try {

			final long start = System.currentTimeMillis();

			LOGGER.info("Trying to create a new web driver for " + requestedUrl);
			webDriver = new RemoteWebDriver(new URL(SELENIUM_URL), DesiredCapabilities.chrome());

			LOGGER.info("Web driver " + webDriver.getSessionId() + " sucessfully created for " + requestedUrl);

			webDriver.get(requestedUrl);

			LOGGER.info("Web driver " + webDriver.getSessionId() + " is getting content for " + requestedUrl);

			//sleep(TIME_TO_WAIT_FOR_RENDER);

			LOGGER.info("Web driver " + webDriver.getSessionId() + "  finished rendering " + requestedUrl + " in "
					+ (System.currentTimeMillis() - start) + " ms");

			String content = updatePageSource(webDriver.getPageSource());

			LOGGER.info("Web driver " + webDriver.getSessionId() + "  will return content for " + requestedUrl);

			return content;

		} catch (Exception e) {

			LOGGER.warn("Try " + cnt + " for " + requestedUrl + " failed, session " + webDriver.getSessionId()
					+ " failed: " + e.getMessage());
			return render(requestedUrl, cnt++);

		} finally {
			tryToCloseWebDriver(webDriver);
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
