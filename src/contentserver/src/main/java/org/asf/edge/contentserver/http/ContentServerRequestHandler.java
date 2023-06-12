package org.asf.edge.contentserver.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.edge.common.CommonIndexPage;
import org.asf.edge.contentserver.EdgeContentServer;

public class ContentServerRequestHandler extends HttpPushProcessor {

	private String path;
	private File sourceDir;
	private IPreProcessor[] preProcessors;
	private EdgeContentServer server;

	private String sanitizePath(String path) {
		while (path.startsWith("/"))
			path = path.substring(1);
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		while (path.contains("//"))
			path = path.replace("//", "/");
		if (path.contains("\\"))
			path = path.replace("\\", "/");
		if (!path.startsWith("/"))
			path = "/" + path;
		return path;
	}

	public interface IPreProcessor {
		public boolean match(String path, String method, RemoteClient client, String contentType, HttpRequest request,
				HttpResponse response);

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
			setResponseStatus(404, "Not found");
			return;
		} else if (requestedFile.isDirectory()) {
			// Directory
			// Check server settings
			if (server.getConfiguration().allowIndexingAssets) {
				// Index
				CommonIndexPage.index(requestedFile, getRequest(), getResponse());
			} else {
				// Not found
				setResponseStatus(404, "Not found");
			}
			return;
		}

		// Find type
		String type = MainFileMap.getInstance().getContentType(requestedFile);

		// Load file
		InputStream fileStream = new FileInputStream(requestedFile);

		// Find preprocessor
		boolean processed = false;
		for (IPreProcessor processor : preProcessors) {
			if (processor.match(path, method, client, contentType, getRequest(), getResponse())) {
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
