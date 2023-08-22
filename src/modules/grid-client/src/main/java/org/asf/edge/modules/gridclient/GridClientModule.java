package org.asf.edge.modules.gridclient;

import java.io.IOException;

import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.edge.modules.IEdgeModule;
import org.asf.edge.modules.eventbus.EventListener;
import org.asf.edge.modules.gridclient.phoenix.PhoenixEnvironment;
import org.asf.edge.modules.gridclient.phoenix.auth.LoginManager;
import org.asf.edge.modules.gridclient.phoenix.certificate.PhoenixCertificate;
import org.asf.edge.modules.gridclient.phoenix.events.PhoenixGameInvalidatedEvent;

import com.google.gson.JsonObject;

public class GridClientModule implements IEdgeModule {
	public static final String GRID_API_VERSION = "1.0.0.A1";
	public static final String GRID_SOFTWARE_ID = "edge-phoenix-grid";

	@Override
	public String moduleID() {
		return "edge-grid";
	}

	@Override
	public String version() {
		return "1.0.0.A1";
	}

	@Override
	public void init() {
		// Test
		PhoenixEnvironment.defaultAPIServer = "http://localhost:16718/";

		// Read config
		// TODO

		// Authenticate game
		authenticateGame();

		// Test
		LoginManager manager = new LoginManager();
		JsonObject req = new JsonObject();
		req.addProperty("mode", "usernamepassword");
		req.addProperty("username", "tester");
		req.addProperty("password", "tester");
		manager.login(req, t -> {
			t = t;
		}, t -> {
			t = t;
		}, t -> {
			t = t;
		});
		try {
			GridClient.initGridPhoenixClient("127.0.0.1", 16719,
					PhoenixCertificate.downloadFromAPI("nexusgrid", "d825f238-7924-4315-93cd-55a43af2dd08"), manager);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@EventListener
	public void phoenixTokenInvalidated(PhoenixGameInvalidatedEvent event) {
		authenticateGame();
	}

	private void authenticateGame() {
		try {
			// Authenticate
			GridClient.authenticateGame(GRID_API_VERSION, GRID_SOFTWARE_ID);
		} catch (IOException e) {
			// Retry
			AsyncTaskManager.runAsync(() -> {
				while (true) {
					// Authenticate
					try {
						GridClient.authenticateGame(GRID_API_VERSION, GRID_SOFTWARE_ID);
					} catch (IOException e2) {
					}
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e1) {
						break;
					}
				}
			});
		}
	}

}
