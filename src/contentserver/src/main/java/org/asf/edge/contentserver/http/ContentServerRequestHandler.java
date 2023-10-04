package org.asf.edge.contentserver.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.stream.Stream;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.apache.logging.log4j.LogManager;
import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.CommonIndexPage;
import org.asf.edge.common.util.TripleDesUtil;
import org.asf.edge.contentserver.EdgeContentServer;

import com.google.gson.JsonObject;

public class ContentServerRequestHandler extends HttpPushProcessor {

	private String path;
	private File sourceDir;
	private IPreProcessor[] preProcessors;
	private EdgeContentServer server;

	private String sanitizePath(String path) {
		if (path.contains("\\"))
			path = path.replace("\\", "/");
		while (path.startsWith("/"))
			path = path.substring(1);
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		while (path.contains("//"))
			path = path.replace("//", "/");
		if (!path.startsWith("/"))
			path = "/" + path;
		return path;
	}

	public interface IPreProcessor {
		public boolean match(String path, String method, RemoteClient client, String contentType, HttpRequest request,
				HttpResponse response, File sourceDir);

		public InputStream preProcess(String path, String method, RemoteClient client, String contentType,
				HttpRequest request, HttpResponse response, InputStream source, File sourceDir) throws IOException;
	}

	public ContentServerRequestHandler(File sourceDir, String path, IPreProcessor[] preProcessors,
			EdgeContentServer server) {
		this.sourceDir = sourceDir;
		this.path = sanitizePath(path);
		this.server = server;

		this.preProcessors = preProcessors;
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ContentServerRequestHandler(sourceDir, path, preProcessors, server);
	}

	@Override
	public String path() {
		return path;
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		// Compute subpath
		path = sanitizePath(path.substring(this.path.length()));

		// Make sure its not attempting to access a resource outside of the scope
		if (path.startsWith("..") || path.endsWith("..") || path.contains("/..") || path.contains("../")) {
			setResponseStatus(403, "Forbidden");
			return;
		}

		// Find file
		File requestedFile = new File(sourceDir, path);
		if (!requestedFile.exists()) {
			// Not found
			if (server.getConfiguration().fallbackAssetServerEndpoint != null) {
				// Attempt to contact fallback server
				String url = server.getConfiguration().fallbackAssetServerEndpoint;
				while (url.endsWith("/"))
					url = url.substring(0, url.lastIndexOf("/"));
				url += path;

				// Try to contact server
				try {
					// Pull file
					URL u = new URL(url);
					InputStream fileStream = u.openStream();

					// Find type
					String type = MainFileMap.getInstance().getContentType(new File(path).getName());

					// Check file
					boolean encrypted = false;
					byte[] key = null;
					if (path.toLowerCase().endsWith("/dwadragonsmain.xml")) {
						// Read data
						byte[] docData = fileStream.readAllBytes();
						fileStream.close();

						// Decode
						String data = new String(docData, "UTF-8");
						encrypted = data.matches("^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)?$");

						// Compute key
						String secret = "C92EC1AA-54CD-4D0C-A8D5-403FCCF1C0BD";
						if (encrypted) {
							// Find version-specific secret
							File verSpecificSecret = new File(sourceDir, path.split("/")[1] + "/" + path.split("/")[2]
									+ "/" + path.split("/")[3] + "/versionxmlsecret.conf");
							if (verSpecificSecret.exists()) {
								// Read
								for (String line : Files.readAllLines(verSpecificSecret.toPath())) {
									if (!line.isBlank() && !line.startsWith("#") && line.contains("=")) {
										String k = line.substring(0, line.indexOf("="));
										String val = line.substring(line.indexOf("=") + 1);
										if (k.equals("xmlsecret"))
											secret = val;
									}
								}
							}

							// Compute key
							try {
								MessageDigest digest = MessageDigest.getInstance("MD5");
								key = digest.digest(secret.getBytes("ASCII"));
							} catch (NoSuchAlgorithmException e) {
								throw new RuntimeException(e);
							}

							// Decrypt
							byte[] b = Base64.getDecoder().decode(data);
							docData = TripleDesUtil.decrypt(b, key);
							data = new String(docData, "UTF-8");
						}

						// Update
						JsonObject replace = server.getConfiguration().fallbackAssetServerManifestModifications;
						if (replace != null) {
							for (String replaceKey : replace.keySet())
								data = data.replace(replaceKey,
										replace.get(replaceKey).getAsString().replace("%local%",
												"[" + server.getConfiguration().listenAddress + "]:"
														+ server.getConfiguration().listenPort));
							docData = data.getBytes("UTF-8");
						}
						fileStream = new ByteArrayInputStream(docData);
					}

					// Find preprocessor
					for (IPreProcessor processor : preProcessors) {
						if (processor.match(path, method, client, contentType, getRequest(), getResponse(),
								sourceDir)) {
							// Run preprocessor
							fileStream = processor.preProcess(path, method, client, contentType, getRequest(),
									getResponse(), fileStream, sourceDir);
						}
					}

					// Write to disk if needed
					if (server.getConfiguration().storeFallbackAssetDownloads) {
						try {
							// Create output
							requestedFile.getParentFile().mkdirs();
							FileOutputStream fO = new FileOutputStream(requestedFile);
							fileStream.transferTo(fO);
							fileStream.close();
							fO.close();

							fileStream = new FileInputStream(requestedFile);
						} catch (IOException e) {
							// Error
							LogManager.getLogger("CONTENTSERVER").error("Failed to download asset to disk: " + path, e);
						}
					}

					// Re-encrypt if needed
					if (encrypted) {
						byte[] docData = fileStream.readAllBytes();
						fileStream.close();

						// Re-encrypt
						docData = TripleDesUtil.encrypt(docData, key);

						// Set result
						fileStream = new ByteArrayInputStream(
								Base64.getEncoder().encodeToString(docData).getBytes("UTF-8"));
					}

					// Set output
					if (getResponse().hasHeader("Content-Type"))
						type = getResponse().getHeaderValue("Content-Type");
					setResponseContent(type, fileStream);
					return;
				} catch (Exception e) {
				}
			}

			// Still not found
			setResponseStatus(404, "Not found");
			return;
		} else if (requestedFile.isDirectory()) {
			// Directory
			// Check server settings
			if (server.getConfiguration().allowIndexingAssets) {
				// Find index page

				// Find one by extension
				File indexPage = null;
				if (indexPage == null) {
					// Find in directory
					File[] files = requestedFile.listFiles(t -> !t.isDirectory() && t.getName().startsWith("index.")
							&& !t.getName().substring("index.".length()).contains("."));
					if (files.length != 0) {
						if (Stream.of(files).anyMatch(t -> t.getName().equals("index.html")))
							indexPage = new File(requestedFile, "index.html");
						else if (Stream.of(files).anyMatch(t -> t.getName().equals("index.htm")))
							indexPage = new File(requestedFile, "index.htm");
						else
							indexPage = files[0];
					}
				}

				// Check
				if (indexPage != null) {
					// Assign new page
					requestedFile = indexPage;
					path = path + "/" + indexPage.getName();
				} else {
					// Index
					CommonIndexPage.index(requestedFile, getRequest(), getResponse());
					return;
				}
			} else {
				// Not found
				setResponseStatus(404, "Not found");
				return;
			}
		}

		// Find type
		String type = MainFileMap.getInstance().getContentType(requestedFile);

		// Load file
		InputStream fileStream = new FileInputStream(requestedFile);

		// Find preprocessor
		boolean processed = false;
		for (IPreProcessor processor : preProcessors) {
			if (processor.match(path, method, client, contentType, getRequest(), getResponse(), sourceDir)) {
				// Run preprocessor
				fileStream = processor.preProcess(path, method, client, contentType, getRequest(), getResponse(),
						fileStream, sourceDir);
				processed = true;
			}
		}

		// Set output
		if (getResponse().hasHeader("Content-Type"))
			type = getResponse().getHeaderValue("Content-Type");
		if (!processed)
			setResponseContent(type, fileStream, requestedFile.length());
		else
			setResponseContent(type, fileStream);
	}

	private static class MainFileMap extends MimetypesFileTypeMap {
		private static MainFileMap instance;

		private FileTypeMap parent;

		public static MainFileMap getInstance() {
			if (instance == null) {
				instance = new MainFileMap(MimetypesFileTypeMap.getDefaultFileTypeMap());
			}
			return instance;
		}

		public MainFileMap(FileTypeMap parent) {
			this.parent = parent;
			this.addMimeTypes("application/xml	xml");
			this.addMimeTypes("application/json	json");
			this.addMimeTypes("text/ini	ini	ini");
			this.addMimeTypes("text/css	css");
			this.addMimeTypes("text/javascript	js");
			if (new File(".mime.types").exists()) {
				try {
					this.addMimeTypes(Files.readString(Path.of(".mime.types")));
				} catch (IOException e) {
				}
			}
			if (new File("mime.types").exists()) {
				try {
					this.addMimeTypes(Files.readString(Path.of("mime.types")));
				} catch (IOException e) {
				}
			}
		}

		@Override
		public String getContentType(String filename) {
			String type = super.getContentType(filename);
			if (type.equals("application/octet-stream")) {
				type = parent.getContentType(filename);
			}
			return type;
		}
	}

}
