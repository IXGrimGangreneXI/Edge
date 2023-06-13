package org.asf.edge.common.http.cookies;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 
 * HTTP Cookie Container
 * 
 * @author Sky Swimmer
 *
 */
public class Cookie {

	public static enum SameSiteFlag {
		STRICT("Strict"), LAX("Lax"), NONE("None");

		String value = "";

		private SameSiteFlag(String value) {
			this.value = value;
		}
	}

	public static enum CookieFlag {
		SECURE, HTTP_ONLY
	}

	public static enum CookieOption {
		PATH, DOMAIN
	}

	protected String value;
	protected String name;

	protected boolean secure = false;
	protected boolean httpOnly = false;

	protected SameSiteFlag sameSite = null;
	protected Date expires = null;

	protected String path = null;
	protected String domain = null;

	public Cookie setFlag(CookieFlag flag, boolean value) {
		if (flag == CookieFlag.SECURE) {
			secure = value;
		} else if (flag == CookieFlag.HTTP_ONLY) {
			httpOnly = value;
		}
		return this;
	}

	public Cookie setOption(CookieOption option, String value) {
		if (option == CookieOption.PATH) {
			path = value;
		} else if (option == CookieOption.DOMAIN) {
			domain = value;
		}
		return this;
	}

	public Cookie setExpires(Date expiryDate) {
		expires = expiryDate;
		return this;
	}

	public Cookie setSameSite(SameSiteFlag value) {
		sameSite = value;
		return this;
	}

	public String getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	public Cookie setValue(String value) {
		this.value = value;
		return this;
	}

	public static Cookie create(String name) {
		Cookie cookie = new Cookie();
		cookie.name = name;
		return cookie;
	}

	protected void setValue(Cookie input) {
		value = input.getValue();
		if (input.secure != false)
			secure = input.secure;
		if (input.httpOnly != false)
			httpOnly = input.httpOnly;
		if (input.expires != null)
			expires = input.expires;
		if (input.sameSite != null)
			sameSite = input.sameSite;
		if (input.domain != null)
			domain = input.domain;
		if (input.path != null)
			path = input.path;
	}

	public String getCookieString() {
		String name = getName();
		String value = getValue();

		name = name.replace("%", "%25");
		name = name.replace(";", "%3B");
		name = name.replace("=", "%3D");

		value = value.replace("%", "%25");
		value = value.replace(";", "%3B");
		value = value.replace("=", "%3D");

		String str = name + "=" + value;
		if (secure) {
			str += "; Secure";
		}
		if (httpOnly) {
			str += "; HttpOnly";
		}
		if (sameSite != null) {
			str += "; SameSite=" + sameSite.value;
		}
		if (expires != null) {
			str += "; Expires=" + getHttpDate(expires);
		}
		if (path != null) {
			str += "; Path=" + path;
		}
		if (domain != null) {
			str += "; Domain=" + path;
		}

		return str;
	}

	// Adapted from SO answer: https://stackoverflow.com/a/8642463
	public synchronized String getHttpDate(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(date);
	}

}