package org.asf.edge.contentserver.http.postprocessors;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.File;

import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.edge.common.util.TripleDesUtil;
import org.asf.edge.contentserver.http.ContentServerRequestHandler.IPreProcessor;

public class ApplicationManifestPreProcessor implements IPreProcessor {

	@Override
	public boolean match(String path, String method, RemoteClient client, String contentType, HttpRequest request,
			HttpResponse response, File sourceDir) {
		return path.endsWith("/DWADragonsMain.xml") && !new File(sourceDir, path + ".edgeunencrypted").exists();
	}

	@Override
	public InputStream preProcess(String path, String method, RemoteClient client, String contentType,
			HttpRequest request, HttpResponse response, InputStream source, File sourceDir) throws IOException {
		// Compute key
		String secret = "C92EC1AA-54CD-4D0C-A8D5-403FCCF1C0BD";

		// Find version-specific secret
		File verSpecificSecret = new File(sourceDir,
				path.split("/")[1] + "/" + path.split("/")[2] + "/" + path.split("/")[3] + "/versionxmlsecret.conf");
		if (verSpecificSecret.exists()) {
			// Read
			for (String line : Files.readAllLines(verSpecificSecret.toPath())) {
				if (!line.isBlank() && !line.startsWith("#") && line.contains("=")) {
					String key = line.substring(0, line.indexOf("="));
					String val = line.substring(line.indexOf("=") + 1);
					if (key.equals("xmlsecret"))
						secret = val;
				}
			}
		}

		// Compute key
		byte[] key;
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			key = digest.digest(secret.getBytes("ASCII"));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		// Read manifest
		byte[] manifest = source.readAllBytes();
		source.close();

		// Encrypt with triple DES
		manifest = TripleDesUtil.encrypt(manifest, key);

		// Convert to base64
		String base64Manifest = Base64.getEncoder().encodeToString(manifest);

		return new ByteArrayInputStream(base64Manifest.getBytes("ASCII"));
	}

}
