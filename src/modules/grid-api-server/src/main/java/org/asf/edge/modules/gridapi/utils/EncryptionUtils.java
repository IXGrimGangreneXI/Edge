package org.asf.edge.modules.gridapi.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class EncryptionUtils {

	/**
	 * Creates a SSL context from a keystore file and password
	 * 
	 * @param keystore Keystore to load
	 * @param password Keystore password
	 * @return SSLContext instance
	 */
	public static SSLContext createSSLContext(File keystore, char[] password)
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException,
			CertificateException, IOException {
		KeyStore mainStore = KeyStore.getInstance("JKS");
		mainStore.load(new FileInputStream(keystore), password);

		KeyManagerFactory managerFactory = KeyManagerFactory.getInstance("SunX509");
		managerFactory.init(mainStore, password);

		SSLContext cont = SSLContext.getInstance("TLS");
		cont.init(managerFactory.getKeyManagers(), null, null);

		return cont;
	}

	/**
	 * Decodes PEM-encoded byte arrays
	 * 
	 * @param pem PEM string to decode
	 * @return Decoded byte array
	 */
	public static byte[] pemDecode(String pem) {
		String base64 = pem.replace("\r", "");

		// Strip header
		while (base64.startsWith("-"))
			base64 = base64.substring(1);
		while (!base64.startsWith("-"))
			base64 = base64.substring(1);
		while (base64.startsWith("-"))
			base64 = base64.substring(1);

		// Clean data
		base64 = base64.replace("\n", "");

		// Strip footer
		while (base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);
		while (!base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);
		while (base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);

		// Decode and return
		return Base64.getDecoder().decode(base64);
	}

	/**
	 * Encodes data in PEM format
	 * 
	 * @param key  Data to encode
	 * @param type Key type
	 * @return PEM string
	 */
	public static String pemEncode(byte[] key, String type) {
		// Generate header
		String PEM = "-----BEGIN " + type + " KEY-----";

		// Generate payload
		String base64 = new String(Base64.getEncoder().encode(key));

		// Generate PEM
		while (true) {
			PEM += "\n";
			boolean done = false;
			for (int i = 0; i < 64; i++) {
				if (base64.isEmpty()) {
					done = true;
					break;
				}
				PEM += base64.substring(0, 1);
				base64 = base64.substring(1);
			}
			if (base64.isEmpty())
				break;
			if (done)
				break;
		}

		// Append footer
		PEM += "\n";
		PEM += "-----END " + type + " KEY-----";

		// Return PEM data
		return PEM;
	}

	/**
	 * Signs a byte array (SHA256)
	 * 
	 * @param data       Data to sign
	 * @param privateKey Private key to use
	 * @return Signature for the given data
	 */
	public static byte[] sign(byte[] data, PrivateKey privateKey) {
		return sign(data, privateKey, "Sha256");
	}

	/**
	 * Signs a byte array
	 * 
	 * @param data       Data to sign
	 * @param privateKey Private key to use
	 * @param alg        Hash algorithm
	 * @return Signature for the given data
	 */
	public static byte[] sign(byte[] data, PrivateKey privateKey, String alg) {
		try {
			Signature sig = Signature.getInstance(alg + "WithRSA");
			sig.initSign(privateKey);
			sig.update(data);
			return sig.sign();
		} catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Verifies signatures (SHA256)
	 * 
	 * @param data      Data to verify
	 * @param signature Signature
	 * @param publicKey Public key to use
	 * @return True if the given signature is valid, false otherwise
	 */
	public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) {
		return verify(data, signature, publicKey, "Sha256");
	}

	/**
	 * Verifies signatures
	 * 
	 * @param data      Data to verify
	 * @param signature Signature
	 * @param publicKey Public key to use
	 * @param alg       Hash algorithm
	 * @return True if the given signature is valid, false otherwise
	 */
	public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey, String alg) {
		try {
			Signature sig = Signature.getInstance(alg + "WithRSA");
			sig.initVerify(publicKey);
			sig.update(data);
			return sig.verify(signature);
		} catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
			return false;
		}
	}

	/**
	 * Retrieves a token public key by private key
	 * 
	 * @param tokenPrivateKey Token private key
	 * @return PublicKey instance
	 */
	public static PublicKey getPublicKey(PrivateKey tokenPrivateKey) throws InvalidKeySpecException {
		RSAPrivateCrtKey crt = (RSAPrivateCrtKey) tokenPrivateKey;
		KeyFactory fac;
		try {
			fac = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return fac.generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
	}

}
