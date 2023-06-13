package org.asf.edge.commonapi.tools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;

import org.asf.edge.common.util.TripleDesUtil;

public class DesDecrypt {

	public static void main(String[] args) throws IOException {
		Scanner sc = new Scanner(System.in);

		// Read payload
		System.out.print("Payload: ");
		String payloadEncrypted = sc.nextLine();

		// Read secret
		System.out.print("Secret: ");
		String secret = sc.nextLine();

		// Read encoding
		System.out.print("Unicode or ASCII? [U/A] ");
		String encoding = sc.nextLine();
		if (encoding.equalsIgnoreCase("a"))
			encoding = "ASCII";
		else
			encoding = "UTF-16LE";

		// Compute key
		byte[] key;
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			key = digest.digest(secret.getBytes(encoding));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			sc.close();
			throw new RuntimeException(e);
		}

		// Decrypt
		System.out.println(
				new String(TripleDesUtil.decrypt(Base64.getDecoder().decode(payloadEncrypted), key), encoding));

		// End
		sc.close();
	}

}
