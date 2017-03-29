package com.sparender;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestLogger {

	final Logger LOGGER = LoggerFactory.getLogger(RequestLogger.class);

	private final DateFormat dateTimeFormat = createRFC1123DateTimeFormatter();

	public static DateFormat createRFC1123DateTimeFormatter() {
		DateFormat dtf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
		dtf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dtf;
	}

	final GelfConfiguration config;
	final GelfTransport transport;

	static final String GRAYLOG_HOST = App.prop.get("graylog.host").toString();
	static final int GRAYLOG_PORT = Integer.valueOf(App.prop.get("graylog.port").toString());

	public RequestLogger() {
		this.config = new GelfConfiguration(new InetSocketAddress(GRAYLOG_HOST, GRAYLOG_PORT)).transport(GelfTransports.UDP).queueSize(512).connectTimeout(5000).reconnectDelay(1000).tcpNoDelay(true)
				.sendBufferSize(32768);

		this.transport = GelfTransports.create(config);
	}

	public void logAfter(HttpServletRequest request, HttpServletResponse response, String fullUrl, Integer contentLength, Integer bytes, boolean cacheHit, long startTime, String errorMessage){
		log(false, request, response.getStatus(), fullUrl, contentLength, bytes, cacheHit, startTime, errorMessage);
	}

	public void logBefore(HttpServletRequest request, String fullUrl){
		log(true, request, null, fullUrl, 0, 0, false, System.currentTimeMillis(), null);
	}

	private void log(boolean before, HttpServletRequest request, Integer status, String fullUrl, Integer contentLength, Integer bytes, boolean cacheHit, long startTime, String errorMessage){

		try {
			
			long elapsedTime = System.currentTimeMillis() - startTime;

			String remoteClient = request.getRemoteAddr();
			String method = request.getMethod();

			Map<String, Object> keyValueFields = new HashMap<>();

			String versionFormatted = "-";
			
			String message = null;

			String referrer = request.getHeader("referrer");
			String userAgent = request.getHeader("user-agent");
			referrer = referrer == null ? "-" : referrer;
			userAgent = userAgent == null ? "-" : userAgent;

			message = String.format("%s - - [%s] \"%s %s %s\" %d %d  - %d ms \"%s\" \"%s\"", remoteClient, dateTimeFormat.format(new Date(System.currentTimeMillis())), method, fullUrl, versionFormatted, status, contentLength,
					elapsedTime, referrer, userAgent);

			keyValueFields.put("remoteClient", remoteClient);
			keyValueFields.put("method", method);
			keyValueFields.put("before", before);
			keyValueFields.put("after", !before);
			keyValueFields.put("full-url", fullUrl);
			keyValueFields.put("versionFormatted", versionFormatted);
			keyValueFields.put("content-length", contentLength);
			keyValueFields.put("content-bytes", bytes);
			keyValueFields.put("referrer", referrer);
			keyValueFields.put("user-agent", userAgent);
			keyValueFields.put("elpased-time", elapsedTime);
			keyValueFields.put("elpased-time-seconds", (int) ((elapsedTime / 1000.0)));
			keyValueFields.put("hit-cache", cacheHit);
			keyValueFields.put("error-message", errorMessage);
			
			Enumeration<String> requestHeaders = request.getHeaderNames();
			while(requestHeaders.hasMoreElements()){
				String headerName = requestHeaders.nextElement();
				if(!headerName.equals("Cookie") && !headerName.equals("Authorization")){//Not interested in cookies, auth
					keyValueFields.put("request-header-" + headerName.toLowerCase(), request.getHeader(headerName));
				}
			}

			keyValueFields.put("status", status);

			if (status == null) {
			    message += message + " LOGGED BEFORE PROCESSING REQUEST";
				LOGGER.info(message);
			}else if (status >= 500) {
				LOGGER.error(message);
				keyValueFields.put("level", "error");
			} else if (status >= 400) {
				LOGGER.warn(message);
				keyValueFields.put("level", "warning");
			} else {
				LOGGER.info(message);
				keyValueFields.put("level", "info");
			}

			final GelfMessageBuilder builder = new GelfMessageBuilder(message, "sparender@" + InetAddress.getLocalHost().getHostName());
			final GelfMessage gm = builder.additionalFields(keyValueFields).build();
			transport.trySend(gm);

		}catch (Exception e){
			e.printStackTrace();
		}

	}
	

}
