package com.sparender;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.recyclable.ObjectCreationException;
import org.spf4j.recyclable.RecyclingSupplier;
import org.spf4j.recyclable.impl.RecyclingSupplierBuilder;

import java.io.IOException;

/**
 * Simple HTTP Server that renders HTML pages using Selenium.
 * 
 */
public class SeleniumRenderer {

	public static final Integer TIME_TO_WAIT_FOR_RENDER = 2000;

	public static String base = "https://www.nextprot.org";

	final Logger LOGGER = LoggerFactory.getLogger(RequestLogger.class);

	RecyclingSupplier<WebDriver> driverPool = null;

	private int MAX_ATTEMPTS = 10;
	private int POOL_INITIAL_SIZE = Integer.parseInt(App.prop.get("driver.pool"));
	private int POOL_MAX_SIZE = POOL_INITIAL_SIZE;
	
	public SeleniumRenderer() {
		try {
			driverPool = new RecyclingSupplierBuilder(POOL_MAX_SIZE, new WebDriverFactory()).withInitialSize(POOL_INITIAL_SIZE).build();
		} catch (ObjectCreationException e) {
			throw new RuntimeException(e);
		}
	}

	public String render(final String requestedUrl) throws IOException {

		String finalContent = null;
		WebDriver webdriver = null;
		try {

			LOGGER.info("Requesting to render" + requestedUrl);
			final long start = System.currentTimeMillis();

			int retry = 0;
			boolean success = false;
			while ((!success) && (retry < MAX_ATTEMPTS)){
				try {
					LOGGER.info("Trying to get a driver " + requestedUrl);
					webdriver = driverPool.get();
					LOGGER.info("Got the driver for " + requestedUrl);
					webdriver.get(requestedUrl);
					success = true;
				}catch (Exception exception){
					driverPool.recycle(webdriver, exception);
					retry++;
					sleep(retry * 1000); //Sleep for retry seconds (if it is the second retry, then sleep 2 seconds)
					LOGGER.error("Failed to retrieve " + requestedUrl + "  retrying ...  " + retry + " " + "class: " + exception.getClass() + exception.getMessage());
				}
			}
			
			LOGGER.info("Finished to driver.get for " + requestedUrl);

			sleep(TIME_TO_WAIT_FOR_RENDER);

			String content = webdriver.getPageSource();


			String contentWithoutJs = content.replaceAll("<script(.|\n)*?</script>", "");
			String contentWithoutJsAndHtmlImport = contentWithoutJs.replaceAll("<link rel=\"import\".*/>", "");
			String contentWithoutJsAndHtmlImportAndIframes = contentWithoutJsAndHtmlImport
					.replaceAll("<iframe .*</iframe>", "");
			String contentWithCorrectBase = contentWithoutJsAndHtmlImportAndIframes.replaceAll("(<base.*?>)",
					"<base href=\"" + base + "\"/>");

			finalContent = contentWithCorrectBase;

			LOGGER.info("Finished rendering " + requestedUrl + " in " + (System.currentTimeMillis() - start) + " ms after " + retry + " retrials");

		}finally {

			driverPool.recycle(webdriver);
		}

		return finalContent;

	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * try { // Waits for active connections to finish (new
	 * WebDriverWait(driver, 50, 1000)).until(new ExpectedCondition<Boolean>() {
	 * public Boolean apply(WebDriver d) { System.err.println("Waiting since " +
	 * (System.currentTimeMillis() - start) + " ms"); // TODO only works with
	 * jQuery now, should be // optimised Object o = ((JavascriptExecutor)
	 * d).executeScript("return ((jQuery)? jQuery.active : 0)"); return
	 * o.equals(0L); } });
	 * 
	 * } catch (org.openqa.selenium.TimeoutException timeout) {
	 * System.err.println("Not finished ... after timeout !!! " ); }
	 */

}
