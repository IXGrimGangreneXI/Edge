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

import org.asf.edge.common.services.items.impl.ItemManagerImpl;
import org.asf.edge.common.util.TripleDesUtil;
import org.asf.edge.common.xmls.items.edgespecific.ItemRegistryManifest;
import org.asf.edge.contentserver.xmls.AssetVersionManifestData;
import org.asf.edge.contentserver.xmls.LoadScreenData;
import org.asf.edge.contentserver.xmls.ProductConfigData;
import org.asf.edge.contentserver.xmls.AssetVersionManifestData.AssetBlockLegacy;
import org.asf.edge.contentserver.xmls.AssetVersionManifestData.AssetVersionBlock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

public class EdgeContentServerDumper {

	private static String[] tdEncryptedFiles = new String[] { "DWADragonsMain.xml" };
	private static String[] retainFiles = new String[0];

	private static boolean dryRun = false;
	private static ArrayList<String> assetsToDownload = new ArrayList<String>();

	private static String versionSecretFile = "\n" + "#\n"
			+ "# This file defines the secret used to encrypt the manifest sent to the client\n"
			+ "# Its version-specific likely, for %ver% its %secret%\n" + "#\n" + "\n" + "xmlsecret=%secret%";

	public static void main(String[] args) throws IOException {
		// Check arguments
		if (args.length < 7) {
			System.err.println(
					"Missing arguments: \"<server>\" \"<version>\" \"<platform>\" \"<key>\" \"<output>\" \"<new-server-url>\" <overwrite true/false> [<dry run true/false>] [\"<file name to retain>\" ...]");
			System.err.println(
					"Example arguments: \"http://media.jumpstart.com/\" \"3.31.0\" \"WIN\" \"C92EC1AA-54CD-4D0C-A8D5-403FCCF1C0BD\" \"asset-archive\" \"https://example.com/sod-archive/\" false false");
			System.err.println("");
			System.err.println(
					"The server url can be found in the client's resources.assets, the url before 'DWADragonsMain_SodStaging' is where the DWADragonsMain.xml file is pulled from.");
			System.err.println("The UUID after 'DWADragonsMain_SodStaging' is the secret used to decrypt the xmls.");
			System.err.println("");
			System.err.println(
					"Make sure to remove 'DWADragonsUnity' from the server URL as else asset resolution will fail!");
			System.exit(1);
		}
		if (!args[0].endsWith("/"))
			args[0] += "/";
		if (!args[5].endsWith("/"))
			args[5] += "/";
		if (args.length >= 8)
			dryRun = args[7].equalsIgnoreCase("true");
		retainFiles = new String[args.length - 8];
		for (int i = 8; i < args.length; i++) {
			retainFiles[i - 8] = args[i];
		}

		// Create output
		if (!dryRun)
			System.out.println("Creating output...");
		File output = new File(args[4], "DWADragonsUnity/" + args[2] + "/" + args[1]);
		if (!dryRun)
			output.mkdirs();
		if (!dryRun)
			System.out.println("Saving to " + output.getPath() + "!");
		else
			System.out.println("Output is " + output.getPath() + "!");
		if (!dryRun) {
			System.out.println("Saving key...");
			Files.writeString(new File(output, "versionxmlsecret.conf").toPath(), versionSecretFile
					.replace("%secret%", args[3]).replace("%plat%", args[2]).replace("%ver%", args[1]));
		}

		// Download serverdown
		File serverDown = new File(args[4], "ServerDown.xml");
		if (!serverDown.exists()) {
			if (!dryRun) {
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
			} else
				assetsToDownload.add("ServerDown.xml");
		}

		// Download manifest
		if (!dryRun)
			System.out.println("Downloading manifests...");
		else
			System.out.println("Verifying manifests...");
		String manifest = downloadString(args[0], args[1], args[2], args[3], "DWADragonsMain.xml", "");

		// Read XML into memory
		System.out.println("Loading manifest...");
		String[] urlsToSwap = new String[] {

				// HTTPS JS media
				"https://media.jumpstart.com/",
				// HTTPS SoD media
				"https://media.schoolofdragons.com/",
				// HTTP JS media
				"http://media.jumpstart.com/",
				// HTTP SoD media
				"http://media.schoolofdragons.com/",

				// User
				args[5]

		};
		String xml = manifest;
		for (String u2 : urlsToSwap) {
			if (xml.contains(u2)) {
				xml = xml.replace(u2, args[0]);
			}
		}

		// Parse manifest
		XmlMapper mapper = new XmlMapper();
		ProductConfigData conf = mapper.readValue(xml, ProductConfigData.class);

		// Modify
		System.out.println("Modifying manifest...");
		xml = manifest;
		urlsToSwap = new String[] {

				// HTTPS JS media
				"https://media.jumpstart.com/",
				// HTTPS SoD media
				"https://media.schoolofdragons.com/",
				// HTTP JS media
				"http://media.jumpstart.com/",
				// HTTP SoD media
				"http://media.schoolofdragons.com/",

				// User
				args[0]

		};
		for (String u2 : urlsToSwap) {
			if (xml.contains(u2)) {
				// Swap it for our new server
				xml = xml.replace(u2, args[5]);
			}
		}

		// Save
		if (!dryRun && !new File(output, "DWADragonsMain.xml").exists())
			Files.writeString(new File(output, "DWADragonsMain.xml").toPath(), xml);

		// Download main asset
		if (conf.manifests != null && conf.manifests.length != 0) {
			if (!dryRun)
				System.out.println("Downloading main assets...");
			else
				System.out.println("Verifying main assets...");
			downloadFileEachQuality(args[0], args[1], args[2], args[3], output, "dwadragonsmain", "",
					"?v=00000000000000000000000000000000", args[6].equalsIgnoreCase("true"));
			for (String man : conf.manifests)
				downloadFileEachQuality(args[0], args[1], args[2], args[3], output, man, "",
						"?v=00000000000000000000000000000000", args[6].equalsIgnoreCase("true"));
		}

		// Compute URL
		URL dataUrl = new URL(conf.dataURL[0].replace("{Version}", args[1]));
		String path = dataUrl.getPath()
				.substring(dataUrl.getPath().indexOf("DWADragonsUnity/" + args[2] + "/" + args[1])
						+ ("DWADragonsUnity/" + args[2] + "/" + args[1]).length() + 1);

		// Download assets
		downloadAssetsForEachQuality(args[0], args[1], args[2], args[3], output, mapper, path + "/AssetVersionsDO.xml",
				conf, new File(args[4]), args[5], args[6].equalsIgnoreCase("true"));

		// Download question files
		if (!dryRun)
			System.out.println("Downloading questiondata assets...");
		else
			System.out.println("Verifying questiondata assets...");
		InputStream strm = EdgeContentServerDumper.class.getClassLoader().getResourceAsStream("questiondata.xml");
		String data = new String(strm.readAllBytes(), "UTF-8");
		strm.close();

		// Go through questions
		QuestionListData[] questions = mapper.reader().readValue(data, QuestionListData[].class);
		for (QuestionListData lst : questions) {
			if (lst.imageURL != null) {
				downloadAsset(lst.imageURL, args[0], new File(args[4]), args[1], args[2], path);
			}
			for (QuestionListData.QuestionBlock q : lst.questions) {
				if (q.imageURL != null) {
					downloadAsset(q.imageURL, args[0], new File(args[4]), args[1], args[2], path);
				}
				for (QuestionListData.QuestionBlock.AnswerBlock a : q.answers) {
					if (a.imageURL != null) {
						downloadAsset(a.imageURL, args[0], new File(args[4]), args[1], args[2], path);
					}
				}
			}
		}

		// Download item data
		if (!dryRun)
			System.out.println("Downloading item assets...");
		else
			System.out.println("Verifying item assets...");

		// Load XML
		strm = ItemManagerImpl.class.getClassLoader().getResourceAsStream("itemdata/itemdefs.xml");
		data = new String(strm.readAllBytes(), "UTF-8");
		strm.close();

		// Go through defs
		ItemRegistryManifest reg = mapper.reader().readValue(data, ItemRegistryManifest.class);

		// Load items
		for (ObjectNode def : reg.itemDefs) {
			// Check item assets
			if (def.has("an")) {
				// Check asset name
				String asset = def.get("an").asText();
				if (asset != null) {
					downloadAsset(asset, args[0], new File(args[4]), args[1], args[2], path);
				}
			}
		}

		// Done!
		if (!dryRun)
			System.out.println("Finished!");
		else {
			System.out.println();
			System.out.println(assetsToDownload.size() + " files to download:");
			for (String file : assetsToDownload) {
				if (file.startsWith("/"))
					file = file.substring(1);
				System.out.println(" - " + file);
			}
		}
	}

	private static void downloadAsset(String asset, String server, File outputRoot, String version, String platform,
			String path) throws IOException {
		// Replace URL
		if (asset.startsWith("https://media.jumpstart.com/"))
			asset = server + asset.substring("https://media.jumpstart.com/".length());
		else if (asset.startsWith("http://media.jumpstart.com/"))
			asset = server + asset.substring("http://media.jumpstart.com/".length());
		else if (asset.startsWith("https://media.schoolofdragons.com/"))
			asset = server + asset.substring("https://media.schoolofdragons.com/".length());
		else if (asset.startsWith("http://media.schoolofdragons.com/"))
			asset = server + asset.substring("http://media.schoolofdragons.com/".length());
		if (asset.startsWith(server)) {
			// Download asset
			URL asU = new URL(asset);
			String path2 = asset.substring(server.length());
			File dest = new File(outputRoot, path2);
			File outputFile = new File(outputRoot, path2 + ".tmp");

			// Download image
			if (dryRun) {
				System.out.println("Will download: " + asset + " -> " + dest.getPath());
				if (asset.startsWith("https://media.jumpstart.com/"))
					assetsToDownload.add(asset.substring("https://media.jumpstart.com/".length()));
				else if (asset.startsWith("http://media.jumpstart.com/"))
					assetsToDownload.add(asset.substring("http://media.jumpstart.com/".length()));
				else if (asset.startsWith("https://media.schoolofdragons.com/"))
					assetsToDownload.add(asset.substring("https://media.schoolofdragons.com/".length()));
				else if (asset.startsWith("http://media.schoolofdragons.com/"))
					assetsToDownload.add(asset.substring("http://media.schoolofdragons.com/".length()));
				else if (asset.startsWith(server))
					assetsToDownload.add(asset.substring(server.length()));
			} else {
				System.out.println("Downloading: " + asset + " -> " + dest.getPath());
				try {
					outputFile.getParentFile().mkdirs();
					InputStream strmI = asU.openStream();
					FileOutputStream fO = new FileOutputStream(outputFile);
					strmI.transferTo(fO);
					fO.close();

					// Finish
					if (dest.exists())
						dest.delete();
					outputFile.renameTo(dest);
				} catch (IOException e) {
					System.err.println("Failure! " + asset + " was not downloaded!");
				}
			}

			// Download for each quality level
			outputFile = new File(outputRoot, "DWADragonsUnity/" + platform + "/" + version
					+ ("/" + path).replace("/Mid/", "/Low/") + "/" + path2 + ".tmp");
			dest = new File(outputRoot, "DWADragonsUnity/" + platform + "/" + version
					+ ("/" + path).replace("/Mid/", "/Low/") + "/" + path2);
			if (dryRun) {
				System.out.println("Will download: " + asset + " -> " + dest.getPath());
				assetsToDownload.add("DWADragonsUnity/" + platform + "/" + version
						+ ("/" + path).replace("/Mid/", "/Low/") + "/" + path2);
			} else {
				System.out.println("Downloading: " + asset + " -> " + dest.getPath());
				try {
					outputFile.getParentFile().mkdirs();
					InputStream strmI = asU.openStream();
					FileOutputStream fO = new FileOutputStream(outputFile);
					strmI.transferTo(fO);
					fO.close();

					// Finish
					if (dest.exists())
						dest.delete();
					outputFile.renameTo(dest);
				} catch (IOException e) {
					System.err.println("Failure! " + asset + " was not downloaded!");
				}
			}
			outputFile = new File(outputRoot,
					"DWADragonsUnity/" + platform + "/" + version + "/" + path + "/" + path2 + ".tmp");
			dest = new File(outputRoot, "DWADragonsUnity/" + platform + "/" + version + "/" + path + "/" + path2);
			if (dryRun) {
				System.out.println("Will download: " + asset + " -> " + dest.getPath());
				assetsToDownload.add("DWADragonsUnity/" + platform + "/" + version + "/" + path + "/" + path2);
			} else {
				System.out.println("Downloading: " + asset + " -> "
						+ new File(outputRoot, "DWADragonsUnity/" + platform + "/" + version + "/" + path + "/" + path2)
								.getPath());
				try {
					outputFile.getParentFile().mkdirs();
					InputStream strmI = asU.openStream();
					FileOutputStream fO = new FileOutputStream(outputFile);
					strmI.transferTo(fO);
					fO.close();

					// Finish
					if (dest.exists())
						dest.delete();
					outputFile.renameTo(dest);
				} catch (IOException e) {
					System.err.println("Failure! " + asset + " was not downloaded!");
				}
			}
			outputFile = new File(outputRoot, "DWADragonsUnity/" + platform + "/" + version
					+ ("/" + path).replace("/Mid/", "/High/") + "/" + path2 + ".tmp");
			dest = new File(outputRoot, "DWADragonsUnity/" + platform + "/" + version
					+ ("/" + path).replace("/Mid/", "/High/") + "/" + path2);
			if (dryRun) {
				System.out.println("Will download: " + asset + " -> " + dest.getPath());
				assetsToDownload.add("DWADragonsUnity/" + platform + "/" + version
						+ ("/" + path).replace("/Mid/", "/High/") + "/" + path2);
			} else {
				System.out.println("Downloading: " + asset + " -> " + dest.getPath());
				try {
					outputFile.getParentFile().mkdirs();
					InputStream strmI = asU.openStream();
					FileOutputStream fO = new FileOutputStream(outputFile);
					strmI.transferTo(fO);
					fO.close();

					// Finish
					if (dest.exists())
						dest.delete();
					outputFile.renameTo(dest);
				} catch (IOException e) {
					System.err.println("Failure! " + asset + " was not downloaded!");
				}
			}
		}
	}

	private static void downloadAssetsForEachQuality(String server, String version, String platform, String key,
			File output, XmlMapper mapper, String manifest, ProductConfigData conf, File outputRoot,
			String newServerURL, boolean overwrite) throws IOException {
		downloadAssets(server, version, platform, key, output, mapper, manifest, "Mid", conf, outputRoot, newServerURL,
				overwrite);
		downloadAssets(server, version, platform, key, output, mapper, manifest, "Low", conf, outputRoot, newServerURL,
				overwrite);
		downloadAssets(server, version, platform, key, output, mapper, manifest, "High", conf, outputRoot, newServerURL,
				overwrite);
	}

	private static void downloadAssets(String server, String version, String platform, String key, File output,
			XmlMapper mapper, String manifest, String level, ProductConfigData conf, File outputRoot,
			String newServerURL, boolean overwrite) throws IOException {
		if (manifest.startsWith("/"))
			manifest = manifest.substring(1);
		// Load asset manifest
		String manData;
		File manFile;
		try {
			System.out.println(
					"Downloading asset list file... Downloading " + manifest.replace("/Mid/", "/" + level + "/"));
			String assetListManTxt = downloadString(server, version, platform, key,
					manifest.replace("/Mid/", "/" + level + "/"), "?v=00000000000000000000000000000000");
			System.out.println(
					"Parsing asset list... Reading file " + manifest.replace("/Mid/", "/" + level + "/") + "...");
			manData = assetListManTxt;
			manFile = new File(output, manifest.replace("/Mid/", "/" + level + "/"));
		} catch (IOException e) {
			System.out.println("Downloading asset list file... Downloading " + manifest);
			String assetListManTxt = downloadString(server, version, platform, key, manifest,
					"?v=00000000000000000000000000000000");
			System.out.println("Parsing asset list... Reading file " + manifest + "...");
			manData = assetListManTxt;
			manFile = new File(output, manifest);
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

		// Read old data
		AssetVersionManifestData oldData = null;
		if (manFile.exists()) {
			oldData = mapper.readValue(Files.readString(manFile.toPath()), AssetVersionManifestData.class);

			// Map legacy versions
			if (oldData.assets == null && oldData.legacyData != null) {
				HashMap<String, AssetVersionBlock> assets = new HashMap<String, AssetVersionBlock>();
				for (AssetBlockLegacy legacyBlock : oldData.legacyData) {
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
				oldData.assets = assets.values().toArray(t -> new AssetVersionBlock[t]);
			}
		} else if (!dryRun)
			Files.writeString(manFile.toPath(), manData); // First time

		// Download all assets
		ArrayList<String> failed = new ArrayList<String>();
		for (AssetVersionManifestData.AssetVersionBlock asset : assetData.assets) {
			for (AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock variant : asset.variants) {
				String url = asset.name;
				url = asset.name;

				// Find old
				AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock old = null;
				if (oldData != null) {
					for (AssetVersionManifestData.AssetVersionBlock a2 : assetData.assets) {
						if (a2.name.equalsIgnoreCase(asset.name)) {
							// Found the asset
							for (AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock v2 : asset.variants) {
								// Check variant
								if ((v2.locale == null && variant.locale == null) || (v2.locale != null
										&& variant.locale != null && v2.locale.equalsIgnoreCase(variant.locale))) {
									// Found it
									old = v2;
									break;
								}
							}
							break;
						}
					}
				}

				// Build url
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
				String path = u.getPath().substring(u.getPath().indexOf("DWADragonsUnity/" + platform + "/" + version)
						+ ("DWADragonsUnity/" + platform + "/" + version).length() + 1);
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
								if (promoUrl.startsWith("RS_DATA/Content/"))
									promoUrl = server + "/" + promoUrl.substring("RS_DATA/".length());
								if (!promoUrl.startsWith("http"))
									continue;
								URL dataUrl = new URL(conf.dataURL[0].replace("{Version}", version));
								String pth = dataUrl.getPath().substring(
										dataUrl.getPath().indexOf("DWADragonsUnity/" + platform + "/" + version)
												+ ("DWADragonsUnity/" + platform + "/" + version).length() + 1);
								downloadAsset(promoUrl, server, outputRoot, version, platform, pth);
							} else if (line.contains("<IconRes>")) {
								String promoUrl = line.substring(line.indexOf("<IconRes>") + "<IconRes>".length());
								promoUrl = promoUrl.substring(0, promoUrl.indexOf("</IconRes>"));
								promoUrl = decodeXML(promoUrl);
								if (promoUrl.startsWith("RS_DATA/Content/"))
									promoUrl = server + "/" + promoUrl.substring("RS_DATA/".length());
								if (!promoUrl.startsWith("http"))
									continue;
								URL dataUrl = new URL(conf.dataURL[0].replace("{Version}", version));
								String pth = dataUrl.getPath().substring(
										dataUrl.getPath().indexOf("DWADragonsUnity/" + platform + "/" + version)
												+ ("DWADragonsUnity/" + platform + "/" + version).length() + 1);
								downloadAsset(promoUrl, server, outputRoot, version, platform, pth);
							} else if (line.contains("<ImageRes>")) {
								String promoUrl = line.substring(line.indexOf("<ImageRes>") + "<ImageRes>".length());
								promoUrl = promoUrl.substring(0, promoUrl.indexOf("</ImageRes>"));
								promoUrl = decodeXML(promoUrl);
								if (promoUrl.startsWith("RS_DATA/Content/"))
									promoUrl = server + "/" + promoUrl.substring("RS_DATA/".length());
								if (!promoUrl.startsWith("http"))
									continue;
								URL dataUrl = new URL(conf.dataURL[0].replace("{Version}", version));
								String pth = dataUrl.getPath().substring(
										dataUrl.getPath().indexOf("DWADragonsUnity/" + platform + "/" + version)
												+ ("DWADragonsUnity/" + platform + "/" + version).length() + 1);
								downloadAsset(promoUrl, server, outputRoot, version, platform, pth);
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
								if (dryRun) {
									System.out.println("Will download: " + screenU + " -> " + dest.getPath());
									assetsToDownload.add(path2);
								} else {
									System.out.println("Downloading: " + screenU + " -> " + dest.getPath());
									outputFile.getParentFile().mkdirs();
									InputStream strmI = screenU.openStream();
									FileOutputStream fO = new FileOutputStream(outputFile);
									strmI.transferTo(fO);
									fO.close();

									// Finish
									if (dest.exists())
										dest.delete();
									outputFile.renameTo(dest);
								}
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
									if (dryRun) {
										System.out.println("Will download: " + promoUrl + " -> " + dest.getPath());
										assetsToDownload.add(path2);
									} else {
										System.out.println("Downloading: " + promoUrl + " -> " + dest.getPath());
										outputFile.getParentFile().mkdirs();
										InputStream strmI = new URL(promoUrl).openStream();
										FileOutputStream fO = new FileOutputStream(outputFile);
										strmI.transferTo(fO);
										fO.close();

										// Finish
										if (dest.exists())
											dest.delete();
										outputFile.renameTo(dest);
									}
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
				if ((!overwrite && (outputFile.exists() && (old != null && old.version <= variant.version)))
						|| (outputFile.exists()
								&& Stream.of(retainFiles).anyMatch(t -> t.equalsIgnoreCase(outputFile.getName())))) {
					System.out.println("Skipped: " + url);
					continue;
				} else if (dryRun) {
					assetsToDownload.add(new URL(url).getPath());
					System.out.println("Will download: " + url + " -> " + outputFile.getPath());
					continue;

				}
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

					// Modify if needed
					if (path.endsWith(".xml")) {
						String[] urlsToSwap = new String[] {

								// HTTPS JS media
								"https://media.jumpstart.com/",
								// HTTPS SoD media
								"https://media.schoolofdragons.com/",
								// HTTP JS media
								"http://media.jumpstart.com/",
								// HTTP SoD media
								"http://media.schoolofdragons.com/",

								// User
								server

						};

						// Read XML into memory
						String xml = Files.readString(outputFile.toPath());
//						if (path.endsWith("/DailyBonusAndPromoDO.xml")) { // FIXME: this is currently broken
//							// Swap out media URLS
//							for (String u2 : urlsToSwap) {
//								if (xml.contains(u2)) {
//									// Swap it for our new server
//									xml = xml.replace(u2, "RS_DATA/");
//								}
//							}
//						} else {
						for (String u2 : urlsToSwap) {
							if (xml.contains(u2)) {
								// Swap it for our new server
								xml = xml.replace(u2, newServerURL);
							}
						}
//						}

						// Save
						Files.writeString(outputFile.toPath(), xml);
					}

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
			for (String url : failed)
				System.err.println(" - " + url);
			System.err.println("");
			System.err.println("");
		}

		// Apply
		if (!dryRun) {
			if (manFile.exists())
				manFile.delete();
			Files.writeString(manFile.toPath(), manData);
		}
	}

	private static void downloadFileEachQuality(String server, String version, String platform, String key, File output,
			String file, String outputSuffix, String query, boolean overwrite)
			throws MalformedURLException, IOException {
		downloadFile(server, version, platform, key, output, "Low/" + file, outputSuffix, query, overwrite);
		downloadFile(server, version, platform, key, output, "Mid/" + file, outputSuffix, query, overwrite);
		downloadFile(server, version, platform, key, output, "High/" + file, outputSuffix, query, overwrite);
	}

	private static String downloadString(String server, String version, String platform, String key, String file,
			String query) throws MalformedURLException, IOException {
		// Build url
		String url = server + "DWADragonsUnity/" + platform + "/" + version + "/" + file + query;
		System.out.println("Downloading: " + url);
		if (dryRun)
			assetsToDownload.add("DWADragonsUnity/" + platform + "/" + version + "/" + file);

		// Download
		String fileF = file;
		InputStream strm = new URL(url).openStream();
		if (Stream.of(tdEncryptedFiles).anyMatch(t -> t.equals(fileF))) {
			// Check if this is a encrypted manifest
			String man = new String(strm.readAllBytes(), "UTF-8");
			strm.close();
			if (man.matches("^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)?$")) {
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
				byte[] data = Base64.getDecoder().decode(man);
				strm = new ByteArrayInputStream(TripleDesUtil.decrypt(data, keyHash));
			} else {
				// Mark exception for this file
				strm = new ByteArrayInputStream(man.getBytes("UTF-8"));
			}
		}

		// Read
		byte[] data = strm.readAllBytes();

		// Close stream
		strm.close();

		// Return
		return new String(data, "UTF-8");
	}

	private static void downloadFile(String server, String version, String platform, String key, File output,
			String file, String outputSuffix, String query, boolean overwrite)
			throws MalformedURLException, IOException {
		// Build url
		String url = server + "DWADragonsUnity/" + platform + "/" + version + "/" + file + query;
		File outputFile = new File(output, file + outputSuffix);
		if (!overwrite && outputFile.exists()) {
			System.out.println("Skipped: " + url);
			return;
		} else if (dryRun) {
			System.out.println("Will download: " + url + " -> " + outputFile.getPath());
			assetsToDownload.add("DWADragonsUnity/" + platform + "/" + version + "/" + file);
			return;
		}
		outputFile.getParentFile().mkdirs();
		System.out.println("Downloading: " + url + " -> " + outputFile.getPath());

		// Download
		String fileF = file;
		InputStream strm = new URL(url).openStream();
		if (Stream.of(tdEncryptedFiles).anyMatch(t -> t.equals(fileF))) {
			// Check if this is a encrypted manifest
			String man = new String(strm.readAllBytes(), "UTF-8");
			strm.close();
			if (man.matches("^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)?$")) {
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
				byte[] data = Base64.getDecoder().decode(man);
				strm = new ByteArrayInputStream(TripleDesUtil.decrypt(data, keyHash));
			} else {
				// Mark exception for this file
				strm = new ByteArrayInputStream(man.getBytes("UTF-8"));
				new File(output, file + ".edgeunencrypted").createNewFile();
			}
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

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
	public static class QuestionListData {

		@JsonProperty("ID")
		public int id;

		public String imageURL;

		@JsonProperty("Qs")
		@JacksonXmlElementWrapper(useWrapping = false)
		public QuestionBlock[] questions = new QuestionBlock[0];

		@JsonIgnoreProperties(ignoreUnknown = true)
		@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
		public static class QuestionBlock {

			@JsonProperty("ID")
			public int id;

			@JsonProperty("Img")
			public String imageURL;

			@JacksonXmlElementWrapper(useWrapping = false)
			public AnswerBlock[] answers = new AnswerBlock[0];

			@JsonIgnoreProperties(ignoreUnknown = true)
			@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
			public static class AnswerBlock {

				@JsonProperty("ID")
				public int id;

				@JsonProperty("Img")
				public String imageURL;

			}

		}

	}

}
