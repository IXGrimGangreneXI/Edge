package org.asf.razorwhip.sentinel.launcher.software.projectedge;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.razorwhip.sentinel.launcher.LauncherUtils;
import org.asf.razorwhip.sentinel.launcher.api.IEmulationSoftwareProvider;
import org.asf.razorwhip.sentinel.launcher.assets.ActiveArchiveInformation;
import org.asf.razorwhip.sentinel.launcher.assets.AssetInformation;
import org.asf.razorwhip.sentinel.launcher.descriptors.data.LauncherController;
import org.asf.razorwhip.sentinel.launcher.descriptors.data.ServerEndpoints;
import org.asf.razorwhip.sentinel.launcher.experiments.ExperimentManager;
import org.asf.razorwhip.sentinel.launcher.software.projectedge.windows.LaunchOptionMenu;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EdgeEmulationSoftware implements IEmulationSoftwareProvider {

	private JsonObject launchSettings;
	private Process serverProc;
	private boolean serverExited;

	public static boolean updating;
	public static boolean showLog;

	@Override
	public void init() {
		try {
			File launchSettingsF = new File("edgelauncher.json");
			if (!launchSettingsF.exists()) {
				// Create
				launchSettings = new JsonObject();
				launchSettings.addProperty("launchMode", "normal");
				JsonObject remoteEndpoints = new JsonObject();
				remoteEndpoints.addProperty("gameplay", "http://localhost:5320/");
				remoteEndpoints.addProperty("common", "http://localhost:5321/");
				remoteEndpoints.addProperty("social", "http://localhost:5322/");
				remoteEndpoints.addProperty("smartfoxHost", "localhost");
				remoteEndpoints.addProperty("smartfoxPort", 5323);
				launchSettings.add("remoteEndpoints", remoteEndpoints);
				Files.writeString(launchSettingsF.toPath(), launchSettings.toString());
			} else {
				// Load
				launchSettings = JsonParser.parseString(Files.readString(launchSettingsF.toPath())).getAsJsonObject();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Register experiments
		registerExperiments();
	}

	@Override
	public void showOptionWindow() {
		// Show option menu
		LaunchOptionMenu window = new LaunchOptionMenu(LauncherUtils.getLauncherWindow());
		window.setVisible(true);

		// TODO:
		// Main menu:
		// - More options:
		// - - Manual game descriptor update
		// - - Manual emulation software update
		// - - Game data transfer

		// Reload
		init();
	}

	@Override
	public void prepareLaunchWithStreamingAssets(String assetArchiveURL, AssetInformation[] collectedAssets,
			AssetInformation[] allAssets, File assetModifications, ActiveArchiveInformation archive,
			JsonObject archiveDef, JsonObject descriptorDef, String clientVersion, File clientDir,
			Runnable successCallback, Consumer<String> errorCallback) {
		prepareLaunch(successCallback, errorCallback);
	}

	@Override
	public void prepareLaunchWithLocalAssets(AssetInformation[] collectedAssets, AssetInformation[] allAssets,
			File assetModifications, ActiveArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef,
			String clientVersion, File clientDir, Runnable successCallback, Consumer<String> errorCallback) {
		prepareLaunch(successCallback, errorCallback);
	}

	private void prepareLaunch(Runnable successCallback, Consumer<String> errorCallback) {
		// Check start mode
		LauncherUtils.log("Loading configuration...");
		if (new File("sentinel.edge.automatedupdate").exists()) {
			updating = true;
			new File("sentinel.edge.automatedupdate").delete();
		}

		// Load remote endpoints
		JsonObject remoteEndpoints = launchSettings.get("remoteEndpoints").getAsJsonObject();
		ServerEndpoints endpointsRemote = new ServerEndpoints();
		endpointsRemote.achievementServiceEndpoint = remoteEndpoints.get("gameplay").getAsString();
		endpointsRemote.commonServiceEndpoint = remoteEndpoints.get("common").getAsString();
		endpointsRemote.contentserverServiceEndpoint = remoteEndpoints.get("gameplay").getAsString();
		endpointsRemote.groupsServiceEndpoint = remoteEndpoints.get("social").getAsString();
		endpointsRemote.itemstoremissionServiceEndpoint = remoteEndpoints.get("gameplay").getAsString();
		endpointsRemote.messagingServiceEndpoint = remoteEndpoints.get("social").getAsString();
		endpointsRemote.userServiceEndpoint = remoteEndpoints.get("common").getAsString();
		endpointsRemote.smartFoxHost = remoteEndpoints.get("smartfoxHost").getAsString();
		endpointsRemote.smartFoxPort = remoteEndpoints.get("smartfoxPort").getAsInt();

		// Load local endpoints
		ServerEndpoints endpointsLocal = new ServerEndpoints();

		// Load edge config
		File edgeConfig = new File("server/server.json");
		File socialSrvJar = new File("server/libs/socialserver.jar");
		if (edgeConfig.exists()) {
			try {
				JsonObject configData = JsonParser.parseString(Files.readString(edgeConfig.toPath())).getAsJsonObject();

				// Load common server configuration
				JsonObject cApiJson = configData.get("commonApiServer").getAsJsonObject();
				String commonURL = (cApiJson.get("https").getAsBoolean() ? "https://" : "http://");
				String ip = cApiJson.get("listenAddress").getAsString();
				if (ip.equals("0.0.0.0"))
					ip = "localhost";
				if (ip.contains(":"))
					commonURL += "[";
				commonURL += ip;
				if (ip.contains(":"))
					commonURL += "]";
				commonURL += ":";
				commonURL += cApiJson.get("listenPort").getAsInt();

				// Load gameplay server configuration
				JsonObject gpApiJson = configData.get("gameplayApiServer").getAsJsonObject();
				String gpURL = (gpApiJson.get("https").getAsBoolean() ? "https://" : "http://");
				ip = gpApiJson.get("listenAddress").getAsString();
				if (ip.equals("0.0.0.0"))
					ip = "localhost";
				if (ip.contains(":"))
					gpURL += "[";
				gpURL += ip;
				if (ip.contains(":"))
					gpURL += "]";
				gpURL += ":";
				gpURL += gpApiJson.get("listenPort").getAsInt();

				// Load social server configuration
				if (configData.has("socialApiServer") && socialSrvJar.exists()) {
					JsonObject sApiJson = configData.get("socialApiServer").getAsJsonObject();
					String sURL = (sApiJson.get("https").getAsBoolean() ? "https://" : "http://");
					ip = sApiJson.get("listenAddress").getAsString();
					if (ip.equals("0.0.0.0"))
						ip = "localhost";
					if (ip.contains(":"))
						sURL += "[";
					sURL += ip;
					if (ip.contains(":"))
						sURL += "]";
					sURL += ":";
					sURL += sApiJson.get("listenPort").getAsInt();

					// Apply
					endpointsLocal.groupsServiceEndpoint = sURL;
					endpointsLocal.messagingServiceEndpoint = sURL;
				} else {
					// Use common API to work around issues with 1.x when there are no social
					// services in the current Edge server files
					endpointsLocal.groupsServiceEndpoint = commonURL;
					endpointsLocal.messagingServiceEndpoint = commonURL;
				}

				// Apply
				endpointsLocal.contentserverServiceEndpoint = gpURL;
				endpointsLocal.itemstoremissionServiceEndpoint = gpURL;
				endpointsLocal.achievementServiceEndpoint = gpURL;
				endpointsLocal.commonServiceEndpoint = commonURL;
				endpointsLocal.userServiceEndpoint = commonURL;

				// Load smartfox server config
				JsonObject mmoSrvJson = configData.get("mmoServer").getAsJsonObject();
				ip = mmoSrvJson.get("listenAddress").getAsString();
				if (ip.equals("0.0.0.0"))
					ip = "localhost";
				endpointsLocal.smartFoxHost = ip;
				endpointsLocal.smartFoxPort = mmoSrvJson.get("listenPort").getAsInt();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else if (!socialSrvJar.exists()) {
			// Use common API to work around issues with 1.x when there are no social
			// services in the current Edge server files
			endpointsLocal.groupsServiceEndpoint = "http://localhost:5321/";
			endpointsLocal.messagingServiceEndpoint = "http://localhost:5321/";
		}

		// Log
		LauncherUtils.log("Determining launch mode...");
		String launchMode = launchSettings.get("launchMode").getAsString();

		// Prepare client startup
		if (launchMode.equals("server") || (updating && launchMode.equals("normal"))) {
			LauncherUtils.addTag("no_launch_client");
		} else {
			// Select endpoints
			if (launchMode.equals("remote-client")) {
				LauncherUtils.addTag("server_endpoints").setValue(ServerEndpoints.class, endpointsRemote);
			} else {
				LauncherUtils.addTag("server_endpoints").setValue(ServerEndpoints.class, endpointsLocal);
			}
		}

		// Check connection
		if (launchMode.equals("remote-client")) {
			try {
				// Open URL connection
				HttpURLConnection conn = (HttpURLConnection) new URL(endpointsRemote.commonServiceEndpoint
						+ (endpointsRemote.commonServiceEndpoint.endsWith("/") ? "" : "/") + "testconnection")
						.openConnection();
				int code = conn.getResponseCode();
				if (code != 200 && code != 404)
					throw new IOException(); // Down
			} catch (Exception e) {
				// Error
				errorCallback.accept("Remote server is not online.");
				return;
			}
		} else if (launchMode.equals("local-client")) {
			try {
				// Open URL connection
				HttpURLConnection conn = (HttpURLConnection) new URL(endpointsLocal.commonServiceEndpoint
						+ (endpointsLocal.commonServiceEndpoint.endsWith("/") ? "" : "/") + "testconnection")
						.openConnection();
				int code = conn.getResponseCode();
				if (code != 200 && code != 404)
					throw new IOException(); // Down
			} catch (Exception e) {
				// Error
				errorCallback.accept("Local server is not online.");
				return;
			}
		}

		// Handle launch mode
		boolean serverLog = launchMode.equals("server");
		if (!launchMode.equals("local-client") && !launchMode.equals("remote-client")) {
			// Check if already active
			boolean alreadyActive = false;
			if (!launchMode.equals("server")) {
				try {
					// Open URL connection
					HttpURLConnection conn = (HttpURLConnection) new URL(endpointsLocal.commonServiceEndpoint
							+ (endpointsLocal.commonServiceEndpoint.endsWith("/") ? "" : "/") + "testconnection")
							.openConnection();
					int code = conn.getResponseCode();
					if (code != 200 && code != 404)
						throw new IOException(); // Down

					// Success
					alreadyActive = true;
				} catch (Exception e) {
				}
			}

			// Check
			if (!alreadyActive) {
				// Start server
				LauncherUtils.log("Starting server...", true);

				// Create process
				String jvm = ProcessHandle.current().info().command().get();

				// Scan libs
				String libs = "globalserver.jar";
				for (File lib : new File("server", "libs").listFiles()) {
					libs += File.pathSeparator + "libs/" + lib.getName();
				}

				// Arguments
				String jvmArgsExtra = "";
				String progArgsExtra = "";
				if (new File("server/commandline.json").exists()) {
					try {
						JsonObject configData = JsonParser
								.parseString(Files.readString(Path.of("server/commandline.json"))).getAsJsonObject();

						// Load
						jvmArgsExtra = configData.get("jvm").getAsString();
						progArgsExtra = configData.get("program").getAsString();
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}
				}

				// Create builder
				ProcessBuilder builder;
				ArrayList<String> args = new ArrayList<String>();
				if (!serverLog && !showLog) {
					// Add classpath arguments
					args.addAll(List.of(jvm, "-cp", libs));

					// Add JVM extra arguments
					if (!jvmArgsExtra.isBlank())
						args.addAll(parseArguments(jvmArgsExtra));

					// Load sentinel update settings
					Map<String, String> descriptor;
					try {
						descriptor = LauncherUtils.parseProperties(
								LauncherUtils.downloadString(LauncherUtils.urlBaseSoftwareFile + "softwareinfo"));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					String listURL = descriptor.get("Update-List-URL");
					if (listURL != null) {
						// Add sentinel update settings
						args.add("-DenableSentinelLauncherUpdateManager=true");
						args.add("-DsentinelLauncherEdgeSoftwareUpdateList=" + listURL);
						args.add("-DsentinelLauncherEdgeSoftwareVersion=" + LauncherUtils.getSoftwareVersion());
					}
					args.add("-DdisableContentServer=true");
					if (System.getProperty("enableAllExperiments") != null)
						args.add("-DenableAllExperiments");
					args.add("-DdisableMmoUnlessExperimentEnabled");

					// Add main class
					args.add("org.asf.edge.globalserver.EdgeGlobalServerMain");
				} else {
					// Add classpath and log arguments
					args.addAll(List.of(jvm, "-cp", libs, "-DopenGuiLog=true"));

					// Add JVM extra arguments
					if (!jvmArgsExtra.isBlank())
						args.addAll(parseArguments(jvmArgsExtra));

					// Load sentinel update settings
					Map<String, String> descriptor;
					try {
						descriptor = LauncherUtils.parseProperties(
								LauncherUtils.downloadString(LauncherUtils.urlBaseSoftwareFile + "softwareinfo"));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					String listURL = descriptor.get("Update-List-URL");
					if (listURL != null) {
						// Add sentinel update settings
						args.add("-DenableSentinelLauncherUpdateManager=true");
						args.add("-DsentinelLauncherEdgeSoftwareUpdateList=" + listURL);
						args.add("-DsentinelLauncherEdgeSoftwareVersion=" + LauncherUtils.getSoftwareVersion());
					}
					args.add("-DdisableContentServer=true");
					if (System.getProperty("enableAllExperiments") != null)
						args.add("-DenableAllExperiments");
					args.add("-DdisableMmoUnlessExperimentEnabled");

					// Add main class
					args.add("org.asf.edge.globalserver.EdgeGlobalServerMain");
				}
				if (!progArgsExtra.isBlank())
					args.addAll(parseArguments(progArgsExtra));
				builder = new ProcessBuilder(args.toArray(t -> new String[t]));
				builder.directory(new File("server"));
				builder.inheritIO();

				// Start
				try {
					// Copy experiment config
					ExperimentManager.getInstance().saveConfig();
					Files.writeString(Path.of("server/experiments.json"),
							Files.readString(Path.of("experiments.json")));

					// Start server
					startServer(builder, launchMode);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				// Wait for server to come online
				while (true) {
					// Check server
					if (serverExited) {
						// Error
						errorCallback.accept("Server exited before the launch was completed!");
						return;
					}

					// Test connection
					try {
						// Open URL connection
						HttpURLConnection conn = (HttpURLConnection) new URL(endpointsLocal.commonServiceEndpoint
								+ (endpointsLocal.commonServiceEndpoint.endsWith("/") ? "" : "/") + "testconnection")
								.openConnection();
						int code = conn.getResponseCode();
						if (code != 200 && code != 404)
							throw new IOException(); // Down

						// Success
						break;
					} catch (Exception e) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							break;
						}
					}
				}
			}
		}

		// Call success
		successCallback.run();
	}

	// Argument parser
	private ArrayList<String> parseArguments(String args) {
		ArrayList<String> args3 = new ArrayList<String>();
		char[] argarray = args.toCharArray();
		boolean ignorespaces = false;
		boolean hasData = false;
		String last = "";
		int i = 0;
		for (char c : args.toCharArray()) {
			if (c == '"' && (i == 0 || argarray[i - 1] != '\\')) {
				if (ignorespaces)
					ignorespaces = false;
				else {
					hasData = true;
					ignorespaces = true;
				}
			} else if (c == ' ' && !ignorespaces && (i == 0 || argarray[i - 1] != '\\')) {
				if (hasData)
					args3.add(last);
				hasData = false;
				last = "";
			} else if (c != '\\' || (i + 1 < argarray.length && argarray[i + 1] != '"'
					&& (argarray[i + 1] != ' ' || ignorespaces))) {
				hasData = true;
				last += c;
			}

			i++;
		}
		if (!last.isEmpty())
			args3.add(last);
		return args3;
	}

	private void startServer(ProcessBuilder builder, String launchMode) throws IOException {
		// Start server
		Process proc = builder.start();
		serverProc = proc;
		AsyncTaskManager.runAsync(() -> {
			// Wait for exit
			int code;
			try {
				code = proc.waitFor();
			} catch (InterruptedException e) {
				code = 1;
			}

			// Handle
			if (code == 237) {
				// Restart server
				try {
					startServer(builder, launchMode);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else if (code == 238) {
				// Restart Sentinel for automatic update
				try {
					// Make sure the next sentinel instance knows its a update
					new File("sentinel.edge.automatedupdate").createNewFile();
				} catch (IOException e) {
				}
				// Check tag
				if (LauncherUtils.hasTag("client_process")) {
					Process clProc = LauncherUtils.getTag("client_process").getValue(Process.class);
					if (clProc.isAlive()) {
						try {
							// Make sure the next sentinel instance shuts the client down if a client update
							// is present, else we can get write errors

							// Create file
							JsonArray procLst = new JsonArray();
							procLst.add(clProc.pid());
							JsonArray fLst = new JsonArray();
							fLst.add("sentinel.edge.automatedupdate");
							JsonObject obj = new JsonObject();
							obj.add("processes", procLst);
							obj.add("deleteFilesOnProcessExit", fLst);
							Files.writeString(Path.of("sentinel.activeprocesses.sjf"), obj.toString());
						} catch (IOException e) {
						}
					}
				}
				System.exit(237);
			} else {
				serverExited = true;
				if (launchMode.equals("server")) {
					// Done
					if (LauncherUtils.hasTag("no_launch_client")) {
						LauncherController cont = LauncherUtils.getTag("no_launch_client")
								.getValue(LauncherController.class);
						if (cont != null)
							cont.exitCallback.run();
					}
				}
			}
		});
	}

	@Override
	public void onGameLaunchSuccess(String version, File clientDir) {
		// Check exit
		if (serverExited) {
			// Exit if needed
			if (LauncherUtils.hasTag("no_launch_client")) {
				LauncherController cont = LauncherUtils.getTag("no_launch_client").getValue(LauncherController.class);
				if (cont != null)
					cont.exitCallback.run();
			}
		}
	}

	@Override
	public void onGameExit(String version, File clientDir) {
		// Check exit
		if (!serverExited && serverProc != null) {
			// Exit
			serverProc.destroy();
		}
	}

	private void registerExperiments() {
		ExperimentManager manager = ExperimentManager.getInstance();

		manager.registerExperiment("EXPERIMENT_LEGACY_INVENTORY_SUPPORT");
		manager.setExperimentName("EXPERIMENT_LEGACY_INVENTORY_SUPPORT", "1.x/2.x inventory enhancements");

		manager.registerExperiment("EXPERIMENT_ACHIEVEMENTSV1_SUPPORT");
		manager.setExperimentName("EXPERIMENT_ACHIEVEMENTSV1_SUPPORT",
				"Support for V1 achievement system (gameplay rewards)");

		manager.registerExperiment("EXPERIMENT_DT_ITEM_SUPPORT");
		manager.setExperimentName("EXPERIMENT_DT_ITEM_SUPPORT", "Support for Dragon Tactics items");

//		manager.registerExperiment("EXPERIMENT_MMO_SERVER_SUPPORT");
//		manager.setExperimentName("EXPERIMENT_MMO_SERVER_SUPPORT",
//				"Multiplayer server support (EXTREMELY WIP, LAN ONLY AT THE MOMENT)");
	}

}
