package com.sparender;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class WebDriverFactory extends BasePooledObjectFactory<RemoteWebDriver> {

	static final String SELENIUM_URL = App.prop.get("selenium.url");
	final Logger LOGGER = LoggerFactory.getLogger(RequestLogger.class);

	@Override
	public RemoteWebDriver create() throws Exception {

		RemoteWebDriver driver = new RemoteWebDriver(new URL(SELENIUM_URL), DesiredCapabilities.chrome());
		LOGGER.info("Creating new remote driver: session "+driver.getSessionId());
		return driver;
	}

	@Override
	public PooledObject<RemoteWebDriver> wrap(RemoteWebDriver webDriver) {

		return new DefaultPooledObject<>(webDriver);
	}
}