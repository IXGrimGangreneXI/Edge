package org.asf.edge.modules.gridclient.phoenix;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.apache.logging.log4j.LogManager;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.modules.eventbus.EventBus;
import org.asf.edge.modules.gridclient.phoenix.events.PhoenixGameInvalidatedEvent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Phoenix Environment Settings
 * 
 * @author Sky Swimmer
 * 
 */
public class PhoenixEnvironment {

	/**
	 * Default API server
	 */
	public static String defaultAPIServer = "https://grid.sentinel.projectedge.net:16718/";

	/**
	 * Phoenix login token
	 */
	public static String defaultLoginToken = null;

	static {
		// Token refresher
		AsyncTaskManager.runAsync(() -> {
			while (true) {
				if (defaultLoginToken != null) {
					// Parse token
					try {
						String[] parts = defaultLoginToken.split("\\.");
						String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), "UTF-8");
						JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();
						try {
							if ((System.currentTimeMillis() / 1000) + (15 * 60) >= payload.get("exp").getAsLong()) {
								// Refresh token
								String url = defaultAPIServer;
								if (!url.endsWith("/"))
									url += "/";
								url += "tokens/refresh";

								// Refresh token
								HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
								conn.setRequestProperty("Authorization", "Bearer " + defaultLoginToken);
								String tkn = new String(conn.getInputStream().readAllBytes(), "UTF-8");
								if (!tkn.isEmpty()) {
									defaultLoginToken = tkn.trim();
								}
							}
						} catch (IOException e) {
							defaultLoginToken = null;
							LogManager.getLogger().error("Phoenix login token is no longer valid!");
							EventBus.getInstance().dispatchEvent(new PhoenixGameInvalidatedEvent());
						}
					} catch (Exception e) {
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		});
	}

}
