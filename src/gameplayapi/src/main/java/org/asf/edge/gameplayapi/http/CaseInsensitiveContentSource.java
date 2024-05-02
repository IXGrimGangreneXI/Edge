package org.asf.edge.gameplayapi.http;

import java.io.IOException;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.ContentSource;
import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.connective.processors.HttpRequestProcessor;

public class CaseInsensitiveContentSource extends ContentSource {

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

	@Override
	public boolean process(String path, HttpRequest request, HttpResponse response, RemoteClient client,
			ConnectiveHttpServer server) throws IOException {
		// Load handlers
		boolean compatible = false;
		HttpRequestProcessor[] processors = server.getAllRequestProcessors();

		// Find handler
		if (request.hasRequestBody()) {
			HttpPushProcessor impl = null;
			for (HttpRequestProcessor p : processors) {
				if (!(p instanceof HttpPushProcessor))
					continue;
				HttpPushProcessor proc = (HttpPushProcessor) p;
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
				for (HttpRequestProcessor p : processors) {
					if (!(p instanceof HttpPushProcessor))
						continue;
					HttpPushProcessor proc = (HttpPushProcessor) p;
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
			for (HttpRequestProcessor proc : processors) {
				if (proc instanceof HttpPushProcessor && !((HttpPushProcessor) proc).supportsNonPush())
					continue;
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
				for (HttpRequestProcessor proc : processors) {
					if (proc instanceof HttpPushProcessor && !((HttpPushProcessor) proc).supportsNonPush())
						continue;
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
