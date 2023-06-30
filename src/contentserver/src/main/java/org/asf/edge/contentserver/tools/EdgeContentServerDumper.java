package org.asf.edge.contentserver.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.stream.Stream;

import org.asf.edge.common.util.TripleDesUtil;
import org.asf.edge.contentserver.xmls.AssetVersionManifestData;
import org.asf.edge.contentserver.xmls.LoadScreenData;
import org.asf.edge.contentserver.xmls.ProductConfigData;
import org.asf.edge.contentserver.xmls.AssetVersionManifestData.AssetBlockLegacy;
import org.asf.edge.contentserver.xmls.AssetVersionManifestData.AssetVersionBlock;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class EdgeContentServerDumper {

	private static String[] tdEncryptedFiles = new String[] { "DWADragonsMain.xml" };

	private static String versionSecretFile = "\n" + "#\n"
			+ "# This file defines the secret used to encrypt the manifest sent to the client\n"
			+ "# Its version-specific likely, for %ver% its %secret%\n" + "#\n" + "\n" + "xmlsecret=%secret%";

	public static void main(String[] args) throws IOException {
		// Check arguments
		if (args.length < 5) {
			System.err.println("Missing arguments: \"<server>\" \"<version>\" \"<platform>\" \"<key>\" \"<output>\"");
			System.err.println(
					"Example arguments: \"http://media.jumpstart.com/\" \"3.31.0\" \"WIN\" \"C92EC1AA-54CD-4D0C-A8D5-403FCCF1C0BD\" \"asset-archive\"");
			System.err.println("");
			System.err.println(
					"The server url can be found in the client's resources.assets, the url before 'DWADragonsMain_SodStaging' is where the DWADragonsMain.xml file is pulled from.");
			System.err.println("The UUID after 'DWADragonsMain_SodStaging' is the secret used to decrypt the xmls.");
			System.err.println("");
			System.err
					.println("Make sure to remove 'DWADragonsUnity' from the server URL as else resolution will fail!");
			System.exit(1);
		}
		if (!args[0].endsWith("/"))
			args[0] += "/";

		// Create output
		System.out.println("Creating output...");
		File output = new File(args[4], "DWADragonsUnity/" + args[2] + "/" + args[1]);
		output.mkdirs();
		System.out.println("Saving to " + output.getPath() + "!");
		System.out.println("Saving key...");
		Files.writeString(new File(output, "versionxmlsecret.conf").toPath(),
				versionSecretFile.replace("%secret%", args[3]).replace("%plat%", args[2]).replace("%ver%", args[1]));

		// Download serverdown
		File serverDown = new File(args[4], "ServerDown.xml");
		if (!serverDown.exists()) {
			// Build url
			String url = args[0] + "ServerDown.xml";
			File outputFile = serverDown;
			outputFile.getParentFile().mkdirs();
			System.out.println("Downloading: " + url + " -> " + outputFile.getPath());

			// Open output
			InputStream strm = new URL(url).openStream();
			FileOutputStream fO = new FileOutputStream(outputFile);
			strm.transferTo(fO);
			fO.close();
		}

		// Download manifest
		System.out.println("Downloading manifest...");
		downloadFile(args[0], args[1], args[2], args[3], output, "DWADragonsMain.xml", "");

		// Parse manifest
		XmlMapper mapper = new XmlMapper();
		System.out.println("Loading manifest...");
		ProductConfigData conf = mapper.readValue(Files.readString(new File(output, "DWADragonsMain.xml").toPath()),
				ProductConfigData.class);

		// Download main asset
		if (conf.manifests != null && conf.manifests.length != 0) {
			System.out.println("Downloading main assets...");
			downloadFileEachQuality(args[0], args[1], args[2], args[3], output, "dwadragonsmain",
					"?v=00000000000000000000000000000000");
			for (String man : conf.manifests)
				downloadFileEachQuality(args[0], args[1], args[2], args[3], output, man,
						"?v=00000000000000000000000000000000");
		}

		// Compute URL
		URL dataUrl = new URL(conf.dataURL[0].replace("{Version}", args[1]));
		String path = dataUrl.getPath().substring(("DWADragonsUnity/" + args[2] + "/" + args[1]).length() + 1);

		// Download assets
		downloadAssetsForEachQuality(args[0], args[1], args[2], args[3], output, mapper, path + "/AssetVersionsDO.xml",
				conf, new File(args[4]));

		// Done!
		System.out.println("Finished!");
	}

	private static void downloadAssetsForEachQuality(String server, String version, String platform, String key,
			File output, XmlMapper mapper, String manifest, ProductConfigData conf, File outputRoot)
			throws IOException {
		downloadAssets(server, version, platform, key, output, mapper, manifest, "Mid", conf, outputRoot);
		downloadAssets(server, version, platform, key, output, mapper, manifest, "Low", conf, outputRoot);
		downloadAssets(server, version, platform, key, output, mapper, manifest, "High", conf, outputRoot);
	}

	private static void downloadAssets(String server, String version, String platform, String key, File output,
			XmlMapper mapper, String manifest, String level, ProductConfigData conf, File outputRoot)
			throws IOException {
		// Load asset manifest
		String manData;
		try {
			System.out.println(
					"Downloading asset list file... Downloading " + manifest.replace("/Mid/", "/" + level + "/"));
			downloadFile(server, version, platform, key, output, manifest.replace("/Mid/", "/" + level + "/"),
					"?v=00000000000000000000000000000000");
			System.out.println(
					"Parsing asset list... Reading file " + manifest.replace("/Mid/", "/" + level + "/") + "...");
			manData = Files.readString(new File(output, manifest.replace("/Mid/", "/" + level + "/")).toPath());
		} catch (IOException e) {
			System.out.println("Downloading asset list file... Downloading " + manifest);
			downloadFile(server, version, platform, key, output, manifest, "?v=00000000000000000000000000000000");
			System.out.println("Parsing asset list... Reading file " + manifest + "...");
			manData = Files.readString(new File(output, manifest).toPath());
		}
		AssetVersionManifestData assetData = mapper.readValue(manData, AssetVersionManifestData.class);

		// Map legacy versions
		if (assetData.assets == null && assetData.legacyData != null) {
			HashMap<String, AssetVersionBlock> assets = new HashMap<String, AssetVersionBlock>();
			for (AssetBlockLegacy legacyBlock : assetData.legacyData) {
				AssetVersionBlock block = null;
				if (assets.containsKey(legacyBlock.assetName)) {
					block = assets.get(legacyBlock.assetName);
				} else {
					block = new AssetVersionBlock();
					block.name = legacyBlock.assetName;
					block.variants = new AssetVersionBlock.AssetVariantBlock[0];
					assets.put(block.name, block);
				}

				// Create variant
				AssetVersionBlock.AssetVariantBlock var = new AssetVersionBlock.AssetVariantBlock();
				var.locale = null;
				var.version = legacyBlock.version;
				var.size = legacyBlock.size;

				// Add to array
				ArrayList<AssetVersionBlock.AssetVariantBlock> lst = new ArrayList<AssetVersionBlock.AssetVariantBlock>(
						Arrays.asList(block.variants));
				lst.add(var);
				block.variants = lst.toArray(t -> new AssetVersionBlock.AssetVariantBlock[t]);
			}
			assetData.assets = assets.values().toArray(t -> new AssetVersionBlock[t]);
		}

		// Download all assets
		ArrayList<String> failed = new ArrayList<String>();
		for (AssetVersionManifestData.AssetVersionBlock asset : assetData.assets) {
			// Build url
			for (AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock variant : asset.variants) {
				String url = asset.name;
				url = asset.name;
				if (url.startsWith("RS_CONTENT/"))
					url = conf.contentDataURL[0].replace("/Mid/", "/" + level + "/").replace("{Version}", version)
							+ url.substring("RS_CONTENT".length());
				else if (url.startsWith("RS_DATA/"))
					url = conf.dataURL[0].replace("/Mid/", "/" + level + "/").replace("{Version}", version)
							+ url.substring("RS_DATA".length());
				else if (url.startsWith("RS_SCENE/"))
					url = conf.sceneURL[0].replace("/Mid/", "/" + level + "/").replace("{Version}", version)
							+ url.substring("RS_SCENE".length());
				else if (url.startsWith("RS_SHARED/"))
					url = conf.sharedDataURL[0].replace("/Mid/", "/" + level + "/").replace("{Version}", version)
							+ url.substring("RS_SHARED".length());
				else if (url.startsWith("RS_SOUND/"))
					url = conf.soundURL[0].replace("/Mid/", "/" + level + "/").replace("{Version}", version)
							+ url.substring("RS_SOUND".length());
				else if (url.startsWith("RS_MOVIES/"))
					url = conf.moviesURL[0].replace("/Mid/", "/" + level + "/").replace("{Version}", version)
							+ url.substring("RS_MOVIES".length());
				else
					throw new IOException("Unable to translate path: " + url); // ?
				URL u = new URL(url);

				// Compute path
				String path = u.getPath().substring(("DWADragonsUnity/" + platform + "/" + version).length() + 1);
				if (variant.locale != null) {
					if (assetData.legacyData == null) {
						File f = new File(path);
						if (f.getName().contains(".")) {
							String oPth = path;
							String ext = f.getName().substring(f.getName().lastIndexOf("."));
							String fn = path.substring(0, path.lastIndexOf("."));
							path = fn + "." + variant.locale + ext;
							url = url.replace(oPth, path);
							u = new URL(url);
						} else {
							String oPth = path;
							path = path + "." + variant.locale;
							url = url.replace(oPth, path);
							u = new URL(url);
						}
					} else {
						path = path.replace("/en-US/", "/" + variant.locale + "/");
					}
				}

				// Check file
				String nm = new File(asset.name).getName();
				try {
					if (nm.equals("DailyBonusAndPromoDO.xml")) {
						// Parse promos
						InputStream strm = new URL(url).openStream();
						String xml = new String(strm.readAllBytes(), "UTF-8");
						strm.close();

						// Scrape instead of parse, a lot of promo images were disabled bc of shutdown
						// day but are still there and the archivist that i am at times says get what
						// you can lol

						// IK NOT PRETTY

						for (String line : xml.replace("\r", "").split("\n")) {
							if (line.contains("<BkgIconRes>")) {
								String promoUrl = line
										.substring(line.indexOf("<BkgIconRes>") + "<BkgIconRes>".length());
								promoUrl = promoUrl.substring(0, promoUrl.indexOf("</BkgIconRes>"));
								promoUrl = decodeXML(promoUrl);

								try {
									// Get path
									URL promoU = new URL(promoUrl);
									String path2 = promoU.getPath();
									File dest = new File(outputRoot, path2);
									if (dest.exists())
										continue;
									File outputFile = new File(outputRoot, path2 + ".tmp");

									// Download image
									System.out.println("Downloading: " + promoUrl + " -> "
											+ new File(outputRoot, path2).getPath());
									outputFile.getParentFile().mkdirs();
									InputStream strmI = new URL(promoUrl).openStream();
									FileOutputStream fO = new FileOutputStream(outputFile);
									strmI.transferTo(fO);
									fO.close();

									// Finish
									if (dest.exists())
										dest.delete();
									outputFile.renameTo(dest);
								} catch (IOException e) {
									System.err.println("Failure! " + promoUrl + " was not downloaded!");
									failed.add(promoUrl);
								}
							} else if (line.contains("<IconRes>")) {
								String promoUrl = line.substring(line.indexOf("<IconRes>") + "<IconRes>".length());
								promoUrl = promoUrl.substring(0, promoUrl.indexOf("</IconRes>"));
								promoUrl = decodeXML(promoUrl);
								if (!promoUrl.startsWith("http"))
									continue;

								try {
									// Get path
									URL promoU = new URL(promoUrl);
									String path2 = promoU.getPath();
									File dest = new File(outputRoot, path2);
									if (dest.exists())
										continue;
									File outputFile = new File(outputRoot, path2 + ".tmp");

									// Download image
									System.out.println("Downloading: " + promoUrl + " -> "
											+ new File(outputRoot, path2).getPath());
									outputFile.getParentFile().mkdirs();
									InputStream strmI = new URL(promoUrl).openStream();
									FileOutputStream fO = new FileOutputStream(outputFile);
									strmI.transferTo(fO);
									fO.close();

									// Finish
									if (dest.exists())
										dest.delete();
									outputFile.renameTo(dest);
								} catch (IOException e) {
									System.err.println("Failure! " + promoUrl + " was not downloaded!");
									failed.add(promoUrl);
								}
							} else if (line.contains("<ImageRes>")) {
								String promoUrl = line.substring(line.indexOf("<ImageRes>") + "<ImageRes>".length());
								promoUrl = promoUrl.substring(0, promoUrl.indexOf("</ImageRes>"));
								promoUrl = decodeXML(promoUrl);
								if (!promoUrl.startsWith("http"))
									continue;

								try {
									// Get path
									URL promoU = new URL(promoUrl);
									String path2 = promoU.getPath();
									File dest = new File(outputRoot, path2);
									if (dest.exists())
										continue;
									File outputFile = new File(outputRoot, path2 + ".tmp");

									// Download image
									System.out.println("Downloading: " + promoUrl + " -> "
											+ new File(outputRoot, path2).getPath());
									outputFile.getParentFile().mkdirs();
									InputStream strmI = new URL(promoUrl).openStream();
									FileOutputStream fO = new FileOutputStream(outputFile);
									strmI.transferTo(fO);
									fO.close();

									// Finish
									if (dest.exists())
										dest.delete();
									outputFile.renameTo(dest);
								} catch (IOException e) {
									System.err.println("Failure! " + promoUrl + " was not downloaded!");
									failed.add(promoUrl);
								}
							}
						}
					} else if (nm.equals("LoadScreenDataDO.xml")) {
						// Parse load screen data
						InputStream strm = new URL(url).openStream();
						String xml = new String(strm.readAllBytes(), "UTF-8");
						strm.close();

						// Parse xml
						LoadScreenData screenData = mapper.readValue(xml, LoadScreenData.class);
						for (LoadScreenData.LoadScreenBlock block : screenData.loadScreens) {
							try {
								// Get path
								URL screenU = new URL(block.name);
								String path2 = screenU.getPath();
								File dest = new File(outputRoot, path2);
								if (dest.exists())
									continue;
								File outputFile = new File(outputRoot, path2 + ".tmp");

								// Download image
								System.out.println(
										"Downloading: " + screenU + " -> " + new File(outputRoot, path2).getPath());
								outputFile.getParentFile().mkdirs();
								InputStream strmI = screenU.openStream();
								FileOutputStream fO = new FileOutputStream(outputFile);
								strmI.transferTo(fO);
								fO.close();

								// Finish
								if (dest.exists())
									dest.delete();
								outputFile.renameTo(dest);
							} catch (IOException e) {
								System.err.println("Failure! " + block.name + " was not downloaded!");
								failed.add(block.name);
							}
						}
					} else if (nm.equals("LoginContentDO.xml")) {
						// Parse promos
						InputStream strm = new URL(url).openStream();
						String xml = new String(strm.readAllBytes(), "UTF-8");
						strm.close();

						// Scrape instead of parse, a lot of promos were disabled bc of shutdown
						// day but are still there and the archivist that i am at times says get what
						// you can lol

						// IK NOT PRETTY

						for (String line : xml.replace("\r", "").split("\n")) {
							if (line.contains("<URL>")) {
								String promoUrl = line.substring(line.indexOf("<URL>") + "<URL>".length());
								promoUrl = promoUrl.substring(0, promoUrl.indexOf("</URL>"));
								promoUrl = decodeXML(promoUrl);

								try {
									// Get path
									URL promoU = new URL(promoUrl);
									String path2 = promoU.getPath();
									File dest = new File(outputRoot, path2);
									if (dest.exists())
										continue;
									File outputFile = new File(outputRoot, path2 + ".tmp");

									// Download image
									System.out.println("Downloading: " + promoUrl + " -> " + outputFile.getPath());
									outputFile.getParentFile().mkdirs();
									InputStream strmI = new URL(promoUrl).openStream();
									FileOutputStream fO = new FileOutputStream(outputFile);
									strmI.transferTo(fO);
									fO.close();

									// Finish
									if (dest.exists())
										dest.delete();
									outputFile.renameTo(dest);
								} catch (IOException e) {
									System.err.println("Failure! " + promoUrl + " was not downloaded!");
									failed.add(promoUrl);
								}
							}
						}
					}
				} catch (IOException e) {
					System.err.println("Failure! " + url + " was not processed!");
					failed.add(url);
				}
				if (!url.endsWith(".xml"))
					url += "?v=" + variant.version;

				// Compute output
				File outputFile = new File(output, path);
				if ((outputFile.exists() && outputFile.length() == variant.size))
					continue;
				outputFile.getParentFile().mkdirs();
				System.out.println("Downloading: " + url + " -> " + outputFile.getPath());
				try {
					// Download
					InputStream strm = new URL(url).openStream();
					String pathF = path;
					if (Stream.of(tdEncryptedFiles).anyMatch(t -> t.equals(pathF))) {
						// Decrypt this file

						// Compute key
						byte[] keyHash;
						try {
							MessageDigest digest = MessageDigest.getInstance("MD5");
							keyHash = digest.digest(key.getBytes("UTF-8"));
						} catch (NoSuchAlgorithmException e) {
							throw new RuntimeException(e);
						}

						// Read data
						byte[] data = Base64.getDecoder().decode(strm.readAllBytes());
						strm.close();
						strm = new ByteArrayInputStream(TripleDesUtil.decrypt(data, keyHash));
					}

					// Open output
					FileOutputStream fO = new FileOutputStream(outputFile);
					strm.transferTo(fO);
					fO.close();

					// Close stream
					strm.close();
				} catch (IOException e) {
					System.err.println("Failure! " + url + " was not downloaded!");
					failed.add(url);
				}
			}
		}
		if (failed.size() != 0) {
			System.err.println("");
			System.err.println("");
			System.err.println("There were failed downloads!");
			for (

			String url : failed)
				System.err.println(" - " + url);
			System.err.println("");
			System.err.println("");
		}
	}

	private static void downloadFileEachQuality(String server, String version, String platform, String key, File output,
			String file, String query) throws MalformedURLException, IOException {
		downloadFile(server, version, platform, key, output, "Low/" + file, query);
		downloadFile(server, version, platform, key, output, "Mid/" + file, query);
		downloadFile(server, version, platform, key, output, "High/" + file, query);
	}

	private static void downloadFile(String server, String version, String platform, String key, File output,
			String file, String query) throws MalformedURLException, IOException {
		// Build url
		String url = server + "DWADragonsUnity/" + platform + "/" + version + "/" + file + query;
		File outputFile = new File(output, file);
		outputFile.getParentFile().mkdirs();
		System.out.println("Downloading: " + url + " -> " + outputFile.getPath());

		// Download
		String fileF = file;
		InputStream strm = new URL(url).openStream();
		if (Stream.of(tdEncryptedFiles).anyMatch(t -> t.equals(fileF))) {
			// Decrypt this file

			// Compute key
			byte[] keyHash;
			try {
				MessageDigest digest = MessageDigest.getInstance("MD5");
				keyHash = digest.digest(key.getBytes("UTF-8"));
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}

			// Read data
			byte[] data = Base64.getDecoder().decode(strm.readAllBytes());
			strm.close();
			strm = new ByteArrayInputStream(TripleDesUtil.decrypt(data, keyHash));
		}

		// Open output
		FileOutputStream fO = new FileOutputStream(outputFile);
		strm.transferTo(fO);
		fO.close();

		// Close stream
		strm.close();
	}

	private static String decodeXML(String content) {
		String newContent = "";

		String buffer = "";
		for (char ch : content.toCharArray()) {
			if (buffer.isEmpty()) {
				if (ch == '&') {
					buffer += ch;
				} else {
					newContent += ch;
				}
			} else {
				if ((buffer + ch).startsWith("&#x")) {
					if (ch != ';') {
						buffer += ch;
					} else {
						buffer = buffer.substring(3);
						try {
							int i = Integer.parseInt(buffer, 16);
							if (i > Character.MAX_VALUE || i < Character.MIN_VALUE)
								throw new NumberFormatException();

							newContent += (char) i;
						} catch (NumberFormatException e) {
							newContent += "&#x" + buffer + ch;
						}
						buffer = "";
					}
				} else {
					if (!(buffer + ch).equals("&#")) {
						newContent += buffer;
						buffer = "";
					} else
						buffer += ch;
				}
			}
		}

		newContent = newContent.replace("&amp;", "&");
		newContent = newContent.replace("&lt;", "<");
		newContent = newContent.replace("&gt;", ">");

		newContent = newContent.replace("&quot;", "\"");
		newContent = newContent.replace("&apos;", ";");

		if (!buffer.isEmpty())
			newContent += buffer;

		return newContent;
	}

}
