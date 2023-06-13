package org.asf.edge.common.http.cookies;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;

/**
 * 
 * HTTP Cookie Collection
 * 
 * @author Sky Swimmer
 *
 */
public class CookieManager {
	protected static CookieManager implementation = new CookieManager();

	protected CookieContext getCookiesImpl(HttpRequest request, HttpResponse response) {
		final HashMap<String, String> cookies = new HashMap<String, String>();
		final HashMap<String, String> outputCookies = new HashMap<String, String>();

		// Parse request cookies
		if (request.hasHeader("Cookie")) {
			String[] cookieString = request.getHeaderValue("Cookie").split("; ");
			for (String cookie : cookieString) {
				if (cookie.isEmpty())
					continue;

				String name = cookie.substring(0, cookie.indexOf("="));
				String value = cookie.substring(cookie.indexOf("=") + 1);

				name = name.replace("%3B", ";");
				name = name.replace("%3D", "=");
				name = name.replace("%25", "%");

				value = value.replace("%3B", ";");
				value = value.replace("%3D", "=");
				value = value.replace("%25", "%");

				cookies.put(name, value);
			}
		}

		// Return
		return getCookies(name -> {
			// Retrieve cookie
			return cookies.get(name);
		}, () -> {
			// Retrieve cookie names
			return cookies.keySet().toArray(t -> new String[t]);
		}, (cookie) -> {
			// Set cookie
			cookies.put(cookie.getName(), cookie.getValue());

			// Check if the cookie was already set
			if (outputCookies.containsKey(cookie.getName())) {
				String[] cookieHeaders = response.getHeaderValues("Set-Cookie");

				// Go through old cookie header data
				for (String headerValue : cookieHeaders) {
					String headerValStr = headerValue;
					String name = headerValue;
					String value = "";

					// Parse cookie
					if (name.contains("=")) {
						value = name.substring(name.indexOf("=") + 1);
						name = name.substring(0, name.indexOf("="));
					}
					if (value.contains("; ")) {
						value = value.substring(0, value.indexOf("; "));
					}

					// Unescape name and value
					name = name.replace("%", "%25");
					name = name.replace(";", "%3B");
					name = name.replace("=", "%3D");
					value = value.replace("%", "%25");
					value = value.replace(";", "%3B");
					value = value.replace("=", "%3D");

					// Check header
					if (name.equals(cookie.getName())) {
						// Remove old value
						response.getHeader("Set-Cookie").removeValue(headerValStr);

						// Add new value
						response.getHeader("Set-Cookie").addValue(cookie.getCookieString());
						return;
					}
				}

				// Add cookie
				response.addHeader("Set-Cookie", cookie.getCookieString(), true);
			} else {
				// Add cookie
				response.addHeader("Set-Cookie", cookie.getCookieString(), true);
			}
		});
	}

	protected CookieContext getCookiesImpl(Function<String, String> cookieProvider, Supplier<String[]> allCookies,
			Consumer<Cookie> cookieOutput) {
		CookieContext collection = new CookieContext();
		collection.assign(cookieOutput);

		for (String cookie : allCookies.get()) {
			collection.assign(cookie, new StringCookie(cookieProvider.apply(cookie)));
		}

		return collection;
	}

	/**
	 * Retrieves the cookie context for the HTTP request and response.
	 * 
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @return Cookie context for the request and response
	 */
	public static CookieContext getCookies(HttpRequest request, HttpResponse response) {
		return implementation.getCookiesImpl(request, response);
	}

	public static CookieContext getCookies(Function<String, String> cookieProvider, Supplier<String[]> allCookies,
			Consumer<Cookie> cookieOutput) {
		return implementation.getCookiesImpl(cookieProvider, allCookies, cookieOutput);
	}
}
