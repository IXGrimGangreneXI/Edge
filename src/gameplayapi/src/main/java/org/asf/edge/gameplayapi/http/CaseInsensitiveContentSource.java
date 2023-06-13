package org.asf.edge.gameplayapi.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.ContentSource;
import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.processors.HttpRequestProcessor;

public class CaseInsensitiveContentSource extends ContentSource {

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

	@Override
	public boolean process(String path, HttpRequest request, HttpResponse response, RemoteClient client,
			ConnectiveHttpServer server) throws IOException {
		// Load handlers
		ArrayList<HttpRequestProcessor> reqProcessors = new ArrayList<HttpRequestProcessor>(
				Arrays.asList(server.getRequestProcessors()));
		ArrayList<HttpPushProcessor> pushProcessors = new ArrayList<HttpPushProcessor>(
				Arrays.asList(server.getPushProcessors()));
		boolean compatible = false;
		for (HttpPushProcessor proc : pushProcessors) {
			if (proc.supportsNonPush()) {
				reqProcessors.add(proc);
			}
		}

		// Find handler
		if (request.hasRequestBody()) {
			HttpPushProcessor impl = null;
			for (HttpPushProcessor proc : pushProcessors) {
				if (!proc.supportsChildPaths()) {
					String url = request.getRequestPath();
					if (!url.endsWith("/"))
						url += "/";

					String supportedURL = proc.path();
					if (!supportedURL.endsWith("/"))
						supportedURL += "/";

					if (url.equalsIgnoreCase(supportedURL)) {
						compatible = true;
						impl = proc;
						break;
					}
				}
			}
			if (!compatible) {
				pushProcessors.sort((t1, t2) -> {
					return -Integer.compare(sanitizePath(t1.path()).split("/").length,
							sanitizePath(t2.path()).split("/").length);
				});
				for (HttpPushProcessor proc : pushProcessors) {
					if (proc.supportsChildPaths()) {
						String url = request.getRequestPath();
						if (!url.endsWith("/"))
							url += "/";

						String supportedURL = sanitizePath(proc.path());
						if (!supportedURL.endsWith("/"))
							supportedURL += "/";

						if (url.toLowerCase().startsWith(supportedURL.toLowerCase())) {
							compatible = true;
							impl = proc;
							break;
						}
					}
				}
			}
			if (compatible) {
				HttpPushProcessor processor = impl.instantiate(server, request, response);
				processor.process(path, request.getRequestMethod(), client, request.getHeaderValue("Content-Type"));
			}
		} else {
			HttpRequestProcessor impl = null;
			for (HttpRequestProcessor proc : reqProcessors) {
				if (!proc.supportsChildPaths()) {
					String url = request.getRequestPath();
					if (!url.endsWith("/"))
						url += "/";

					String supportedURL = proc.path();
					if (!supportedURL.endsWith("/"))
						supportedURL += "/";

					if (url.equalsIgnoreCase(supportedURL)) {
						compatible = true;
						impl = proc;
						break;
					}
				}
			}
			if (!compatible) {
				reqProcessors.sort((t1, t2) -> {
					return -Integer.compare(sanitizePath(t1.path()).split("/").length,
							sanitizePath(t2.path()).split("/").length);
				});
				for (HttpRequestProcessor proc : reqProcessors) {
					if (proc.supportsChildPaths()) {
						String url = request.getRequestPath();
						if (!url.endsWith("/"))
							url += "/";

						String supportedURL = sanitizePath(proc.path());
						if (!supportedURL.endsWith("/"))
							supportedURL += "/";

						if (url.toLowerCase().startsWith(supportedURL.toLowerCase())) {
							compatible = true;
							impl = proc;
							break;
						}
					}
				}
			}
			if (compatible) {
				HttpRequestProcessor processor = impl.instantiate(server, request, response);
				processor.process(path, request.getRequestMethod(), client);
			}
		}

		// Return
		return compatible;
	}

}
