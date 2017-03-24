package com.sparender;

import org.eclipse.jetty.server.Server;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Main class used for debug / IDE Launch Verticle with docker instead (try this
 * for debug) See documentation here:
 * http://vertx.io/docs/vertx-docker/#_deploying_a_java_verticle_in_a_docker_container
 * 
 * @author Daniel Teixeira http://github.com/ddtxra
 *
 */
public class App {

	public static Map<String, String> prop = new HashMap<>();

	public static void main(String[] args) throws Exception {

		Path path = Paths.get("config.properties");

		try (BufferedReader br = Files.newBufferedReader(path)) {
			br.lines().filter(l -> !l.isEmpty()).filter(l -> !l.startsWith("#")).forEach(l -> {
				String[] keyValuePair = l.split("=");
				System.err.println(l);
				prop.put(keyValuePair[0], keyValuePair[1]);
			});
		}

		Server server = new Server(8082);
		server.setHandler(new RequestHandler());

		server.start();
		server.join();
	}
}